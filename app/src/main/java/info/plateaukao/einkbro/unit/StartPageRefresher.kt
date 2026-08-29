package info.plateaukao.einkbro.unit

import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.EBWebView
import java.lang.ref.WeakReference

/**
 * Weak registry of rendered start-page tabs: lets a config change (card
 * layout switch in settings) re-render every open start page right away.
 */
object StartPageRefresher {
    private val webViews = mutableSetOf<WeakReference<EBWebView>>()

    fun register(webView: EBWebView) {
        webViews += WeakReference(webView)
    }

    fun refreshAll() {
        webViews.removeAll { it.get() == null }
        webViews.forEach { ref ->
            val webView = ref.get() ?: return@forEach
            webView.post {
                runCatching {
                    if (webView.url == Constants.START_PAGE_URL) {
                        BookmarkRenderer.loadStartPage(webView)
                    }
                }
            }
        }
    }
}
