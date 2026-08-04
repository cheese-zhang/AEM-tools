package com.github.aemtoolkit.toolwindow.osgi

import com.github.aemtoolkit.server.AemBundle
import com.github.aemtoolkit.server.AemBundleAction
import com.github.aemtoolkit.server.AemBundleService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.io.IOException
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

/**
 * Displays and controls Felix bundles from the configured AEM server.
 */
class AemBundlesPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val model = DefaultListModel<AemBundle>()
    private val list = JBList(model)
    private val status = JLabel("Refresh to load Felix bundles")
    private val search = JBTextField().apply {
        emptyText.text = "Search symbolic name, version, or ID"
    }
    private val stateFilter = ComboBox(BundleStateFilter.entries.toTypedArray())
    private val startButton = JButton("Start")
    private val stopButton = JButton("Stop")
    private val refreshBundleButton = JButton("Refresh Bundle")
    private var bundles: List<AemBundle> = emptyList()

    init {
        val filters = JPanel(BorderLayout(6, 0)).apply {
            add(search, BorderLayout.CENTER)
            add(stateFilter, BorderLayout.EAST)
        }
        add(
            JPanel(BorderLayout(0, 4)).apply {
                add(filters, BorderLayout.NORTH)
                add(status, BorderLayout.SOUTH)
            },
            BorderLayout.NORTH,
        )
        list.cellRenderer = BundleRenderer()
        add(ScrollPaneFactory.createScrollPane(list, true), BorderLayout.CENTER)
        add(
            JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                add(JButton("Reload").apply { addActionListener { refresh() } })
                add(startButton.apply {
                    addActionListener { selectedBundle()?.let { changeState(it, AemBundleAction.START) } }
                })
                add(stopButton.apply {
                    addActionListener { selectedBundle()?.let { changeState(it, AemBundleAction.STOP) } }
                })
                add(refreshBundleButton.apply {
                    addActionListener {
                        selectedBundle()?.let { changeState(it, AemBundleAction.REFRESH) }
                    }
                })
                add(JButton("Fetch JAR").apply {
                    addActionListener { selectedBundle()?.let(::fetchBundle) }
                })
                add(JButton("Open Console").apply {
                    addActionListener {
                        selectedBundle()?.let(AemBundleService.getInstance(project)::openBundle)
                    }
                })
            },
            BorderLayout.SOUTH,
        )
        search.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) = applyFilters()
        })
        stateFilter.addActionListener { applyFilters() }
        list.addListSelectionListener { updateActionState() }
        list.addMouseListener(
            object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(event: java.awt.event.MouseEvent) {
                    if (event.clickCount == 2 && event.button == java.awt.event.MouseEvent.BUTTON1) {
                        selectedBundle()?.let(AemBundleService.getInstance(project)::openBundle)
                    }
                }
            },
        )
        updateActionState()
    }

    /** Reloads Felix bundles without blocking the UI thread. */
    fun refresh() {
        status.text = "Loading Felix bundles..."
        setActionsEnabled(false)
        AppExecutorUtil.getAppExecutorService().execute {
            val result = loadBundles()
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                bundles = result.bundles
                applyFilters()
                status.text = result.error ?: "${bundles.size} Felix bundles"
            }
        }
    }

    private fun applyFilters() {
        val selectedId = selectedBundle()?.id
        val selectedState = (stateFilter.selectedItem as? BundleStateFilter)?.state
        val filtered = bundles.filter {
            AemBundleFilter.matches(it, search.text, selectedState)
        }
        model.clear()
        filtered.forEach(model::addElement)
        selectedId
            ?.let { id -> filtered.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 }
            ?.let { list.selectedIndex = it }
        status.text = "${filtered.size} shown / ${bundles.size} bundles"
        updateActionState()
    }

    private fun changeState(bundle: AemBundle, action: AemBundleAction) {
        if (action != AemBundleAction.REFRESH) {
            val answer = Messages.showYesNoDialog(
                project,
                "${action.label} ${bundle.symbolicName} on the configured AEM instance?",
                "${action.label} Felix Bundle",
                Messages.getQuestionIcon(),
            )
            if (answer != Messages.YES) return
        }
        status.text = "Running ${action.label.lowercase()} for ${bundle.symbolicName}..."
        setActionsEnabled(false)
        AppExecutorUtil.getAppExecutorService().execute {
            val result = try {
                AemBundleService.getInstance(project).changeState(bundle, action)
                loadBundles()
            } catch (error: IOException) {
                BundleLoadResult(emptyList(), error.message ?: "Bundle action failed")
            } catch (error: IllegalArgumentException) {
                BundleLoadResult(emptyList(), error.message ?: "Invalid AEM server URL")
            }
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                if (result.error == null) {
                    bundles = result.bundles
                    applyFilters()
                    status.text = "${action.label} completed for ${bundle.symbolicName}"
                } else {
                    status.text = result.error
                    updateActionState()
                }
            }
        }
    }

    private fun fetchBundle(bundle: AemBundle) {
        status.text = "Downloading ${bundle.symbolicName}..."
        setActionsEnabled(false)
        AppExecutorUtil.getAppExecutorService().execute {
            val error = try {
                AemBundleService.getInstance(project).fetchAndAttach(bundle)
                null
            } catch (failure: IOException) {
                failure.message ?: "Bundle download failed"
            } catch (failure: IllegalStateException) {
                failure.message ?: "Bundle could not be attached"
            }
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                if (error == null) {
                    status.text = "Attached ${bundle.symbolicName} as a project library"
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("AEM Toolkit")
                        .createNotification(
                            "Felix bundle attached",
                            bundle.symbolicName,
                            NotificationType.INFORMATION,
                        )
                        .notify(project)
                } else {
                    status.text = error
                }
                updateActionState()
            }
        }
    }

    private fun loadBundles(): BundleLoadResult =
        try {
            BundleLoadResult(AemBundleService.getInstance(project).getBundles(), null)
        } catch (error: IOException) {
            BundleLoadResult(emptyList(), error.message ?: "Bundle request failed")
        } catch (error: IllegalArgumentException) {
            BundleLoadResult(emptyList(), error.message ?: "Invalid AEM server URL")
        }

    private fun selectedBundle(): AemBundle? = list.selectedValue

    private fun updateActionState() {
        val selected = selectedBundle()
        val state = selected?.state.orEmpty()
        startButton.isEnabled = selected != null && !state.equals("Active", true)
        stopButton.isEnabled = selected != null &&
            (state.equals("Active", true) || state.equals("Starting", true))
        refreshBundleButton.isEnabled = selected != null
    }

    private fun setActionsEnabled(enabled: Boolean) {
        if (!enabled) {
            startButton.isEnabled = false
            stopButton.isEnabled = false
            refreshBundleButton.isEnabled = false
        } else {
            updateActionState()
        }
    }

    private data class BundleLoadResult(
        val bundles: List<AemBundle>,
        val error: String?,
    )

    private enum class BundleStateFilter(
        private val label: String,
        val state: String?,
    ) {
        ALL("All states", null),
        ACTIVE("Active", "Active"),
        RESOLVED("Resolved", "Resolved"),
        INSTALLED("Installed", "Installed"),
        STARTING("Starting", "Starting"),
        STOPPING("Stopping", "Stopping");

        override fun toString(): String = label
    }

    private class BundleRenderer : ColoredListCellRenderer<AemBundle>() {
        override fun customizeCellRenderer(
            list: JList<out AemBundle>,
            value: AemBundle,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean,
        ) {
            append(value.symbolicName)
            value.version?.let {
                append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
            value.state?.let {
                val attributes = if (it.equals("Active", true)) {
                    SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                } else {
                    SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES
                }
                append("  [$it]", attributes)
            }
            append("  #${value.id}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }
}
