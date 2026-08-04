package com.github.aemtoolkit.toolwindow.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile

/**
 * Resolves dialog models for editor-backed files.
 *
 * Callers must hold IntelliJ read access.
 */
object AemDialogEditorSupport {
    /** Returns true when [file] is a Granite dialog content file. */
    fun isDialog(file: VirtualFile?): Boolean =
        file?.name == ".content.xml" &&
            file.path.replace('\\', '/').contains("/_cq_dialog/")

    /** Parses [file] when it is a Granite dialog content file. */
    fun parseDialog(
        project: Project,
        file: VirtualFile?,
        parser: AemDialogStructureParser,
    ): AemDialogNode? =
        file
            ?.takeIf(::isDialog)
            ?.let { PsiManager.getInstance(project).findFile(it) as? XmlFile }
            ?.let(parser::parse)
}
