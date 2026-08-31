package info.plateaukao.einkbro.browser

import android.app.Activity
import android.webkit.JavascriptInterface
import info.plateaukao.einkbro.database.PasswordEntry
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.PasswordStore
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.dialog.SharkPasswordDialogs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * JS bridge ("sharkDock") backing the password manager. Like every WebView
 * interface it is attached to all pages, so each entry point re-checks that
 * the current page is a regular http(s) page before doing anything.
 */
class PasswordBridge(private val webView: EBWebView) : KoinComponent {
    private val coroutineScope: CoroutineScope by inject()
    private val config: ConfigManager by inject()
    private val passwordStore: PasswordStore by inject()

    /** A page containing a password input finished loading: offer one-click fill. */
    @JavascriptInterface
    fun onPasswordPage(origin: String) {
        webView.post {
            if (!isRegularPage()) return@post
            val entry = passwordStore.find(origin) ?: return@post
            if (dismissedOrigins.contains(origin)) return@post
            val activity = webView.context as? Activity ?: return@post
            coroutineScope.launch(Dispatchers.Main) {
                SharkPasswordDialogs.showFillPrompt(activity, webView, origin, entry)
            }
        }
    }

    /** A login form/button was used: offer to save the typed credentials. */
    @JavascriptInterface
    fun onCredentials(origin: String, username: String, password: String) {
        webView.post {
            if (!isRegularPage()) return@post
            if (!config.promptSavePassword) return@post
            if (password.isBlank()) return@post
            if (passwordStore.contains(origin, username, password)) return@post
            val activity = webView.context as? Activity ?: return@post
            coroutineScope.launch(Dispatchers.Main) {
                SharkPasswordDialogs.showSavePrompt(activity, webView, origin, username, password)
            }
        }
    }

    private fun isRegularPage(): Boolean {
        if (webView.incognito) return false
        val url = webView.url ?: return false
        return url.startsWith("http")
    }

    companion object {
        // fill prompt cancelled for these origins until the app restarts
        private val dismissedOrigins = mutableSetOf<String>()

        fun dismissOrigin(origin: String) {
            dismissedOrigins.add(origin)
        }
    }
}
