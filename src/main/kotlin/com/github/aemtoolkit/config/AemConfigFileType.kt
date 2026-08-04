package com.github.aemtoolkit.config

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** File type for AEM ecosystem configuration sources. */
class AemConfigFileType : LanguageFileType(AemConfigLanguage) {
    override fun getName(): String = "AEM Configuration"

    override fun getDescription(): String = "AEM Dispatcher or CND configuration"

    override fun getDefaultExtension(): String = "cnd"

    override fun getIcon(): Icon = IconLoader.getIcon("/icons/aemToolWindow.svg", javaClass)
}
