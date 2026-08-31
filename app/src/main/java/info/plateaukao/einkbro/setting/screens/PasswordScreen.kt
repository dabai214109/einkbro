package info.plateaukao.einkbro.setting.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.EBToast
import info.plateaukao.einkbro.unit.PasswordStore
import info.plateaukao.einkbro.view.dialog.DialogManager
import org.koin.core.context.GlobalContext

/**
 * Saved-credential manager: one row per account (view/copy password, delete),
 * the save-prompt switch and plain-JSON export/import. Exported files keep
 * passwords in clear text on purpose (Keystore keys do not leave the device)
 * — the file must be kept safe.
 */
@Composable
fun PasswordScreen(navController: NavHostController) {
    val activity = LocalContext.current as FragmentActivity
    val config = remember { GlobalContext.get().get<ConfigManager>() }
    val store = remember { GlobalContext.get().get<PasswordStore>() }
    val backupOps = activity as? BackupOps
    val dialogManager = remember { DialogManager(activity) }
    var entries by remember { mutableStateOf(store.all()) }

    LaunchedEffect(Unit) { entries = store.all() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.password_prompt_save),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.body1,
            )
            Switch(
                checked = config.promptSavePassword,
                onCheckedChange = { config.promptSavePassword = it },
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            TextButton(onClick = { backupOps?.exportSharkData() }) {
                Text(stringResource(R.string.password_export))
            }
            TextButton(onClick = { backupOps?.importSharkData() }) {
                Text(stringResource(R.string.password_import))
            }
        }
        Divider()
        LazyColumn {
            items(entries) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = entry.host, style = MaterialTheme.typography.body1)
                        Text(
                            text = entry.username,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    TextButton(onClick = {
                        val plain = store.plainPassword(entry)
                        dialogManager.showOkCancelDialog(
                            title = entry.host + " / " + entry.username,
                            message = plain,
                            okAction = { copyToClipboard(activity, plain) },
                        )
                    }) { Text(stringResource(R.string.password_entry_show)) }
                    TextButton(onClick = {
                        dialogManager.showOkCancelDialog(
                            title = activity.getString(R.string.menu_delete),
                            message = entry.host + " / " + entry.username,
                            okAction = {
                                store.remove(entry.host, entry.username)
                                entries = store.all()
                            },
                        )
                    }) { Text(stringResource(R.string.menu_delete)) }
                }
                Divider()
            }
        }
    }
}

private fun copyToClipboard(activity: FragmentActivity, text: String) {
    val manager = activity.getSystemService(ClipboardManager::class.java)
    manager?.setPrimaryClip(ClipData.newPlainText("password", text))
    EBToast.show(activity, R.string.password_copied_toast)
}
