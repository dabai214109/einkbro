package info.plateaukao.einkbro.preference

import kotlinx.serialization.Serializable

/** A user-curated card on the built-in start page. */
@Serializable
data class StartPageItem(
    val title: String,
    val url: String,
    // pinned cards sort before the rest; insertion order is kept within each group
    val pinned: Boolean = false,
)

/** Card arrangement of the built-in start page. */
enum class StartPageLayout { LIST, GRID_TWO, GRID_THREE }
