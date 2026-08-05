package com.github.aemtoolkit.resolver

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiModificationTracker

/**
 * Resolves every local target represented by an AEM resource type.
 *
 * In addition to repository directories, Granite render-condition resource
 * types can resolve to ordinary Java classes implementing [RENDER_CONDITION_FQN].
 */
@Service(Service.Level.PROJECT)
class AemResourceTypeTargetResolver(private val project: Project) {
    private val renderConditions: CachedValue<Map<String, List<PsiClass>>> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    loadRenderConditions(),
                    PsiModificationTracker.MODIFICATION_COUNT,
                )
            },
            false,
        )

    /** Returns all local repository and Java targets for [resourceType]. */
    fun resolve(resourceType: String): List<PsiElement> {
        val targets = mutableListOf<PsiElement>()
        ResourceTypeResolver.getInstance(project)
            .resolveDirectory(resourceType)
            ?.let { PsiManager.getInstance(project).findDirectory(it) }
            ?.let(targets::add)
        if (!DumbService.isDumb(project)) {
            targets += renderConditions.value[resourceTypeKey(resourceType)].orEmpty()
        }
        return targets.distinctBy { "${it.containingFile?.virtualFile?.path}:${it.textOffset}" }
    }

    /** Returns Java render-condition targets for [resourceType]. */
    fun resolveRenderConditions(resourceType: String): List<PsiClass> =
        if (DumbService.isDumb(project)) {
            emptyList()
        } else {
            renderConditions.value[resourceTypeKey(resourceType)].orEmpty()
        }

    private fun loadRenderConditions(): Map<String, List<PsiClass>> {
        val scope = GlobalSearchScope.projectScope(project)
        val fileIndex = ProjectFileIndex.getInstance(project)
        return PsiShortNamesCache.getInstance(project)
            .allClassNames
            .asSequence()
            .filter { it.endsWith(RENDER_CONDITION_SUFFIX) }
            .flatMap { className ->
                PsiShortNamesCache.getInstance(project)
                    .getClassesByName(className, scope)
                    .asSequence()
            }
            .filter { psiClass ->
                psiClass.containingFile?.virtualFile?.let(fileIndex::isInContent) == true &&
                    InheritanceUtil.isInheritor(psiClass, RENDER_CONDITION_FQN)
            }
            .groupBy { psiClass ->
                normalizeName(psiClass.name.orEmpty().removeSuffix(RENDER_CONDITION_SUFFIX))
            }
    }

    private fun resourceTypeKey(resourceType: String): String =
        normalizeName(
            resourceType
                .removePrefix("/apps/")
                .removePrefix("/libs/")
                .trim('/')
                .substringAfterLast('/'),
        )

    private fun normalizeName(value: String): String =
        value.filter(Char::isLetterOrDigit).lowercase()

    companion object {
        private const val RENDER_CONDITION_FQN =
            "com.adobe.granite.ui.components.rendercondition.RenderCondition"
        private const val RENDER_CONDITION_SUFFIX = "RenderCondition"

        /** Returns the project resource-type target resolver. */
        fun getInstance(project: Project): AemResourceTypeTargetResolver = project.service()
    }
}
