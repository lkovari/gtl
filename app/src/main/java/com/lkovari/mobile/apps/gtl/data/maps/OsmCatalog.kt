package com.lkovari.mobile.apps.gtl.data.maps

data class OsmRegion(
    val id: String,
    val label: String,
    val url: String,
    val countryCode: String,
    val europe: Boolean = false
) {
    fun title(europeWord: String): String {
        return if (europe) "$europeWord $label" else label
    }
}

object OsmCatalog {
    const val MIRROR = "https://ftp-stud.hs-esslingen.de/Mirrors/download.mapsforge.org/maps/v5"
    const val ORIGIN = "https://download.mapsforge.org/maps/v5"

    fun fallbackUrl(url: String): String? {
        if (!url.startsWith(MIRROR)) {
            return null
        }
        return ORIGIN + url.removePrefix(MIRROR)
    }

    val regions: List<OsmRegion> = listOf(
        europe("eu-hungary", "Hungary", "europe/hungary.map", "HU"),
        europe("eu-austria", "Austria", "europe/austria.map", "AT"),
        europe("eu-slovakia", "Slovakia", "europe/slovakia.map", "SK"),
        europe("eu-romania", "Romania", "europe/romania.map", "RO"),
        europe("eu-croatia", "Croatia", "europe/croatia.map", "HR"),
        europe("eu-slovenia", "Slovenia", "europe/slovenia.map", "SI"),
        europe("eu-germany", "Germany", "europe/germany.map", "DE"),
        europe("eu-poland", "Poland", "europe/poland.map", "PL"),
        europe("eu-czech", "Czech Republic", "europe/czech-republic.map", "CZ"),
        europe("eu-italy", "Italy", "europe/italy.map", "IT"),
        europe("eu-france", "France", "europe/france.map", "FR"),
        europe("eu-spain", "Spain", "europe/spain.map", "ES"),
        europe("eu-portugal", "Portugal", "europe/portugal.map", "PT"),
        europe("eu-netherlands", "Netherlands", "europe/netherlands.map", "NL"),
        europe("eu-belgium", "Belgium", "europe/belgium.map", "BE"),
        europe("eu-switzerland", "Switzerland", "europe/switzerland.map", "CH"),
        europe("eu-england", "England", "europe/united-kingdom/england.map", "GB"),
        europe("eu-scotland", "Scotland", "europe/united-kingdom/scotland.map", "GB"),
        europe("eu-wales", "Wales", "europe/united-kingdom/wales.map", "GB"),
        europe("eu-ireland", "Ireland and Northern Ireland", "europe/ireland-and-northern-ireland.map", "IE"),
        europe("eu-greece", "Greece", "europe/greece.map", "GR"),
        europe("eu-sweden", "Sweden", "europe/sweden.map", "SE"),
        europe("eu-norway", "Norway", "europe/norway.map", "NO"),
        europe("eu-finland", "Finland", "europe/finland.map", "FI"),
        europe("eu-denmark", "Denmark", "europe/denmark.map", "DK"),
        europe("eu-ukraine", "Ukraine", "europe/ukraine.map", "UA"),
        europe("eu-serbia", "Serbia", "europe/serbia.map", "RS"),
        europe("eu-bulgaria", "Bulgaria", "europe/bulgaria.map", "BG"),
        OsmRegion("asia-japan", "Japan", "$MIRROR/asia/japan.map", "JP"),
        OsmRegion("asia-india", "India", "$MIRROR/asia/india.map", "IN"),
        OsmRegion("us-california", "US California", "$MIRROR/north-america/us/california.map", "US"),
        OsmRegion("us-new-york", "US New York", "$MIRROR/north-america/us/new-york.map", "US"),
        OsmRegion("us-texas", "US Texas", "$MIRROR/north-america/us/texas.map", "US"),
        OsmRegion("ca-ontario", "CA Ontario", "$MIRROR/north-america/canada/ontario.map", "CA"),
        OsmRegion("sa-brazil", "Brazil", "$MIRROR/south-america/brazil.map", "BR"),
        OsmRegion("au-australia", "Australia", "$MIRROR/australia-oceania/australia.map", "AU")
    )

    private fun europe(id: String, label: String, path: String, countryCode: String): OsmRegion {
        return OsmRegion(id, label, "$MIRROR/$path", countryCode, europe = true)
    }
}
