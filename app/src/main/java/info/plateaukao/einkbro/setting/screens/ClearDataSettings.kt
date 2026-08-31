package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.setting.ActionSettingItem
import info.plateaukao.einkbro.setting.BooleanSettingItem
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.view.EBToast
import kotlinx.coroutines.launch

fun buildClearDataSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        BooleanSettingItem(
            R.string.clear_title_cache,
            0,
            config = config::clearCache,
        ),
        BooleanSettingItem(
            R.string.clear_title_history,
            0,
            config = config::clearHistory,
        ),
        BooleanSettingItem(
            R.string.clear_title_indexedDB,
            0,
            config = config::clearIndexedDB,
        ),
        BooleanSettingItem(
            R.string.clear_title_cookie,
            0,
            R.string.setting_summary_cookie_delete,
            config::clearCookies
        ),
        BooleanSettingItem(
            R.string.clear_title_quit,
            0,
            R.string.clear_summary_quit,
            config::clearWhenQuit
        ),
        ActionSettingItem(
            R.string.clear_title_clear_now,
            0,
            R.string.clear_summary_clear_now,
        ) {
            if (config.clearCache) BrowserUnit.clearCache(deps.activity)
            if (config.clearIndexedDB) BrowserUnit.clearIndexedDB(deps.activity)
            deps.lifecycleScope.launch {
                if (config.clearHistory) BrowserUnit.clearHistory(deps.activity)
                BrowserUnit.clearCookie()
                EBToast.show(deps.activity, R.string.clear_done_toast)
            }
        },
        ActionSettingItem(
            R.string.clear_title_deleteDatabase,
            0,
            R.string.clear_summary_deleteDatabase,
        ) {
            deps.activity.deleteDatabase("Ninja4.db")
            deps.activity.deleteDatabase("pass_DB_v01.db")
            config.restartChanged = true
            deps.activity.finish()
        }
    )
}
