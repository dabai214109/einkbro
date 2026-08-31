package info.plateaukao.einkbro.view.dialog

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.PasswordEntry
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.StartPageItem
import info.plateaukao.einkbro.unit.PasswordStore
import info.plateaukao.einkbro.unit.StartPageRefresher
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.compose.MyTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Native prompts for the password manager: save credentials after a login,
 * and one-click fill when a login page with saved credentials opens. Saving
 * also auto-creates a start page card for first-time sites.
 */
object SharkPasswordDialogs : KoinComponent {
    private val config: ConfigManager by inject()
    private val passwordStore: PasswordStore by inject()

    fun showSavePrompt(
        activity: Activity,
        webView: EBWebView,
        origin: String,
        username: String,
        password: String,
    ) {
        val host = hostOf(origin)
        val userState = mutableStateOf(username)
        val passState = mutableStateOf(password)
        val composeView = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(activity as LifecycleOwner)
            setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
            setContent {
                MyTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 15.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.shark_site_label) + ": " + host,
                            style = MaterialTheme.typography.body2,
                        )
                        OutlinedTextField(
                            value = userState.value,
                            onValueChange = { userState.value = it },
                            label = { Text(stringResource(R.string.shark_username_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = MaterialTheme.colors.onBackground,
                                cursorColor = MaterialTheme.colors.onBackground,
                            ),
                        )
                        OutlinedTextField(
                            value = passState.value,
                            onValueChange = { passState.value = it },
                            label = { Text(stringResource(R.string.shark_password_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = MaterialTheme.colors.onBackground,
                                cursorColor = MaterialTheme.colors.onBackground,
                            ),
                        )
                    }
                }
            }
        }

        DialogManager(activity).showOkCancelDialog(
            title = activity.getString(R.string.shark_save_prompt_title),
            view = composeView,
            okAction = {
                val user = userState.value.trim()
                passwordStore.put(origin, user, passState.value)
                autoCreateCard(host, user)
                EBToast.show(activity, R.string.shark_saved_toast)
            },
        ).allowImeForComposeContent()
    }

    fun showFillPrompt(activity: Activity, webView: EBWebView, origin: String, entry: PasswordEntry) {
        val plain = passwordStore.plainPassword(entry)
        DialogManager(activity).showOkCancelDialog(
            title = activity.getString(R.string.shark_fill_prompt_title),
            message = entry.username,
            okAction = {
                info.plateaukao.einkbro.browser.SharkPasswordManager.fillCredentials(
                    webView,
                    entry.username,
                    plain,
                )
            },
            cancelAction = {
                info.plateaukao.einkbro.browser.PasswordBridge.dismissOrigin(origin)
            },
        )
    }

    // First card for a site is named after the host; further accounts on the
    // same site get a card named after the username.
    private fun autoCreateCard(host: String, username: String) {
        val url = "https://$host"
        val items = config.startPageItems
        val sameHost = items.count { hostOf(it.url) == host }
        val title = if (sameHost == 0 || username.isBlank()) host else username
        if (items.any { it.url == url && it.title == title }) return
        config.startPageItems = items + StartPageItem(title, url)
        StartPageRefresher.refreshAll()
    }

    internal fun hostOf(url: String): String = runCatching {
        java.net.URI(url).host?.lowercase()?.removePrefix("www.") ?: url.lowercase()
    }.getOrDefault(url.lowercase())
}
