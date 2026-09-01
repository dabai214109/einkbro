package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.SettingRoute.About
import info.plateaukao.einkbro.activity.SettingRoute.Search
import info.plateaukao.einkbro.activity.SettingRoute.StartControl
import info.plateaukao.einkbro.activity.SettingRoute.Toolbar
import info.plateaukao.einkbro.activity.SettingRoute.Ui
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.NavigateSettingItem
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.setting.VersionSettingItem

// Slimmed to the modules this tool actually uses; everything else was removed
// on purpose (backup, gestures, behavior, AI, misc, data control, passwords).
fun buildMainSettingItems(): List<SettingItemInterface> = listOf(
    NavigateSettingItem(R.string.setting_title_ui, R.drawable.ic_phone, destination = Ui),
    NavigateSettingItem(
        R.string.setting_title_toolbar,
        R.drawable.ic_toolbar,
        destination = Toolbar
    ),
    NavigateSettingItem(
        R.string.setting_title_start_control,
        R.drawable.icon_earth,
        destination = StartControl
    ),
    NavigateSettingItem(
        R.string.setting_title_search,
        R.drawable.icon_search,
        destination = Search
    ),
    DividerSettingItem(),
    VersionSettingItem(
        R.string.menu_other_info,
        R.drawable.icon_info,
        destination = About,
    ),
)
