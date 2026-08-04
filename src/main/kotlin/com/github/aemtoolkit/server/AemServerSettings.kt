package com.github.aemtoolkit.server

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Project-level AEM server configuration.
 *
 * Passwords are stored in IntelliJ Password Safe, never in project files.
 */
@Service(Service.Level.PROJECT)
@State(name = "AemToolkitServer", storages = [Storage("aemToolkit.xml")])
class AemServerSettings(private val project: Project) :
    PersistentStateComponent<AemServerSettings.State> {
    data class State(
        var baseUrl: String = "http://localhost:4502",
        var username: String = "admin",
        var enabled: Boolean = false,
        var debugPort: Int = 5005,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    /** Returns the configured password from Password Safe. */
    fun getPassword(): String? =
        PasswordSafe.instance.getPassword(credentialsKey())

    /** Stores [password] securely for the current server. */
    fun setPassword(password: String?) {
        val credentials = password
            ?.takeIf(String::isNotEmpty)
            ?.let { Credentials(state.username, it) }
        PasswordSafe.instance.set(credentialsKey(), credentials)
    }

    /** Returns a normalized base URL without a trailing slash. */
    fun normalizedBaseUrl(): String = state.baseUrl.trim().trimEnd('/')

    private fun credentialsKey(): CredentialAttributes =
        CredentialAttributes(
            "AEM Toolkit:${project.locationHash}:${normalizedBaseUrl()}",
            state.username,
        )

    companion object {
        /** Returns project server settings. */
        fun getInstance(project: Project): AemServerSettings = project.service()
    }
}
