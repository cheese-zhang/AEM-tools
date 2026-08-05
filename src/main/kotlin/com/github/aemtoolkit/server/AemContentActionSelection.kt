package com.github.aemtoolkit.server

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile

/** Resolves the exact file targeted by an editor or Project View action. */
object AemContentActionSelection {
    /** Prefers the editor document and rejects ambiguous multi-file selections. */
    fun selectedFile(event: AnActionEvent): VirtualFile? {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val editorFile = editor?.document?.let(FileDocumentManager.getInstance()::getFile)
        return select(
            editorFile,
            editor != null,
            event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY),
            event.getData(CommonDataKeys.VIRTUAL_FILE),
        )
    }

    internal fun select(
        editorFile: VirtualFile?,
        editorContext: Boolean,
        projectFiles: Array<out VirtualFile>?,
        contextFile: VirtualFile?,
    ): VirtualFile? {
        if (editorContext) return editorFile
        return projectFiles?.singleOrNull() ?: contextFile
    }
}
