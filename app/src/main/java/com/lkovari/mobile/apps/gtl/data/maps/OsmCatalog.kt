package com.lkovari.mobile.apps.gtl.data.maps

data class OsmRegion(
    val id: String,
    val label: String,
    val url: String,
    val countryCode: String
)

object OsmCatalog {
    private const val BASE = "https://download.mapsforge.org/maps/v5"

    val regions: List<OsmRegion> = listOf(
        OsmRegion("eu-hungary", "EU Hungary", "$BASE/europe/hungary.map", "HU"),
        OsmRegion("eu-austria", "EU Austria", "$BASE/europe/austria.map", "AT"),
        OsmRegion("eu-slovakia", "EU Slovakia", "$BASE/europe/slovakia.map", "SK"),
        OsmRegion("eu-romania", "EU Romania", "$BASE/europe/romania.map", "RO"),
        OsmRegion("eu-croatia", "EU Croatia", "$BASE/europe/croatia.map", "HR"),
        OsmRegion("eu-slovenia", "EU Slovenia", "$BASE/europe/slovenia.map", "SI"),
        OsmRegion("eu-germany", "EU Germany", "$BASE/europe/germany.map", "DE"),
        OsmRegion("eu-poland", "EU Poland", "$BASE/europe/poland.map", "PL"),
        OsmRegion("eu-czech", "EU Czech Republic", "$BASE/europe/czech_republic.map", "CZ"),
        OsmRegion("eu-italy", "EU Italy", "$BASE/europe/italy.map", "IT"),
        OsmRegion("eu-france", "EU France", "$BASE/europe/france.map", "FR"),
        OsmRegion("eu-spain", "EU Spain", "$BASE/europe/spain.map", "ES"),
        OsmRegion("eu-portugal", "EU Portugal", "$BASE/europe/portugal.map", "PT"),
        OsmRegion("eu-netherlands", "EU Netherlands", "$BASE/europe/netherlands.map", "NL"),
        OsmRegion("eu-belgium", "EU Belgium", "$BASE/europe/belgium.map", "BE"),
        OsmRegion("eu-switzerland", "EU Switzerland", "$BASE/europe/switzerland.map", "CH"),
        OsmRegion("eu-england", "EU England", "$BASE/europe/great_britain/england.map", "GB"),
        OsmRegion("eu-scotland", "EU Scotland", "$BASE/europe/great_britain/scotland.map", "GB"),
        OsmRegion("eu-wales", "EU Wales", "$BASE/europe/great_britain/wales.map", "GB"),
        OsmRegion("eu-ireland", "EU Ireland", "$BASE/europe/ireland.map", "IE"),
        OsmRegion("eu-greece", "EU Greece", "$BASE/europe/greece.map", "GR"),
        OsmRegion("eu-sweden", "EU Sweden", "$BASE/europe/sweden.map", "SE"),
        OsmRegion("eu-norway", "EU Norway", "$BASE/europe/norway.map", "NO"),
        OsmRegion("eu-finland", "EU Finland", "$BASE/europe/finland.map", "FI"),
        OsmRegion("eu-denmark", "EU Denmark", "$BASE/europe/denmark.map", "DK"),
        OsmRegion("eu-ukraine", "EU Ukraine", "$BASE/europe/ukraine.map", "UA"),
        OsmRegion("eu-serbia", "EU Serbia", "$BASE/europe/serbia.map", "RS"),
        OsmRegion("eu-bulgaria", "EU Bulgaria", "$BASE/europe/bulgaria.map", "BG"),
        OsmRegion("asia-japan", "Japan", "$BASE/asia/japan.map", "JP"),
        OsmRegion("asia-india", "India", "$BASE/asia/india.map", "IN"),
        OsmRegion("us-california", "US California", "$BASE/north-america/united-states/california.map", "US"),
        OsmRegion("us-new-york", "US New York", "$BASE/north-america/united-states/new-york.map", "US"),
        OsmRegion("us-texas", "US Texas", "$BASE/north-america/united-states/texas.map", "US"),
        OsmRegion("ca-ontario", "CA Ontario", "$BASE/north-america/canada/ontario.map", "CA"),
        OsmRegion("sa-brazil", "Brazil", "$BASE/south-america/brazil.map", "BR"),
        OsmRegion("au-australia", "Australia", "$BASE/australia-oceania/australia.map", "AU")
    )
}
