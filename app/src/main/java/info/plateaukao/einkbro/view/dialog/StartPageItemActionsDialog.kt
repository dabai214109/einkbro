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
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.StartPageItem
import info.plateaukao.einkbro.unit.BookmarkRenderer
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.compose.MyTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Long-press menu for one start page card: open, pin/unpin, rename, change
 * URL or delete. Changes write straight into ConfigManager and re-render the
 * start page.
 */
class StartPageItemActionsDialog(
    private val ebWebView: EBWebView,
    private val item: StartPageItem,
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val activity: Activity = ebWebView.context as Activity

    suspend fun show() {
        val options = mutableListOf(
            activity.getString(R.string.start_page_open),
            activity.getString(
                if (item.pinned) R.string.start_page_unpin else R.string.start_page_pin
            ),
            activity.getString(R.string.start_page_rename),
            activity.getString(R.string.start_page_change_url),
            activity.getString(R.string.menu_delete),
        )
        when (activity.showPlainListDialog(null, options)) {
            0 -> ebWebView.loadUrl(item.url)
            1 -> {
                config.setStartPageItemPinned(item.url, !item.pinned)
                BookmarkRenderer.loadStartPage(ebWebView)
            }
            2 -> editField(
                R.string.start_page_rename,
                item.title,
                R.string.dialog_title_hint,
            ) { title ->
                if (title.isNotEmpty()) {
                    config.updateStartPageItem(item.url, item.copy(title = title))
                }
                BookmarkRenderer.loadStartPage(ebWebView)
            }
            3 -> editField(
                R.string.start_page_change_url,
                item.url,
                R.string.dialog_url_hint,
            ) { rawUrl ->
                val url = rawUrl.takeIf { it.isNotEmpty() }?.let {
                    if (it.contains("://")) it else "https://$it"
                } ?: return@editField
                config.updateStartPageItem(item.url, item.copy(url = url))
                BookmarkRenderer.loadStartPage(ebWebView)
            }
            4 -> confirmDelete()
        }
    }

    private fun editField(titleRes: Int, initial: String, hintRes: Int, onOk: (String) -> Unit) {
        val state = mutableStateOf(initial)
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
                        OutlinedTextField(
                            value = state.value,
                            onValueChange = { state.value = it },
                            label = { Text(stringResource(hintRes)) },
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
            title = activity.getString(titleRes),
            view = composeView,
            okAction = { onOk(state.value.trim()) },
        ).allowImeForComposeContent()
    }

    private fun confirmDelete() {
        DialogManager(activity).showOkCancelDialog(
            title = activity.getString(R.string.menu_delete),
            message = activity.getString(R.string.start_page_delete_confirm, item.title),
            okAction = {
                config.removeStartPageItem(item.url)
                BookmarkRenderer.loadStartPage(ebWebView)
            },
        )
    }
}
