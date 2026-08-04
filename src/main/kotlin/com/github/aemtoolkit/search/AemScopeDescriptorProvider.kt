package com.github.aemtoolkit.search

import com.intellij.ide.util.scopeChooser.ScopeDescriptor
import com.intellij.ide.util.scopeChooser.ScopeDescriptorProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project

/** Exposes downloaded Felix bundles in Find and Find Usages scope pickers. */
class AemScopeDescriptorProvider : ScopeDescriptorProvider {
    override fun getScopeDescriptors(
        project: Project,
        dataContext: DataContext,
    ): Array<ScopeDescriptor> =
        arrayOf(ScopeDescriptor(AemBundlesSearchScope(project)))
}
