package com.github.aemtoolkit.osgi

import com.intellij.openapi.vfs.VirtualFile

/** A project OSGi configuration correlated by service PID. */
data class OsgiConfiguration(
    val pid: String,
    val runModes: List<String>,
    val file: VirtualFile,
)
