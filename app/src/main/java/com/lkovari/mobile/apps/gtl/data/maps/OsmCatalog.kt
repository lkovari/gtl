package com.lkovari.mobile.apps.gtl.data.maps

data class OsmRegion(
    val id: String,
    val label: String,
    val url: String
)

object OsmCatalog {
    private const val BASE = "https://download.mapsforge.org/maps/v5"

    val regions: List<OsmRegion> = listOf(
        OsmRegion("eu-hungary", "EU Hungary", "$BASE/europe/hungary.map"),
        OsmRegion("eu-austria", "EU Austria", "$BASE/europe/austria.map"),
        OsmRegion("eu-slovakia", "EU Slovakia", "$BASE/europe/slovakia.map"),
        OsmRegion("eu-romania", "EU Romania", "$BASE/europe/romania.map"),
        OsmRegion("eu-croatia", "EU Croatia", "$BASE/europe/croatia.map"),
        OsmRegion("eu-slovenia", "EU Slovenia", "$BASE/europe/slovenia.map"),
        OsmRegion("eu-germany", "EU Germany", "$BASE/europe/germany.map"),
        OsmRegion("eu-poland", "EU Poland", "$BASE/europe/poland.map"),
        OsmRegion("eu-czech", "EU Czech Republic", "$BASE/europe/czech_republic.map"),
        OsmRegion("eu-italy", "EU Italy", "$BASE/europe/italy.map"),
        OsmRegion("eu-france", "EU France", "$BASE/europe/france.map"),
        OsmRegion("eu-spain", "EU Spain", "$BASE/europe/spain.map"),
        OsmRegion("eu-portugal", "EU Portugal", "$BASE/europe/portugal.map"),
        OsmRegion("eu-netherlands", "EU Netherlands", "$BASE/europe/netherlands.map"),
        OsmRegion("eu-belgium", "EU Belgium", "$BASE/europe/belgium.map"),
        OsmRegion("eu-switzerland", "EU Switzerland", "$BASE/europe/switzerland.map"),
        OsmRegion("eu-england", "EU England", "$BASE/europe/great_britain/england.map"),
        OsmRegion("eu-scotland", "EU Scotland", "$BASE/europe/great_britain/scotland.map"),
        OsmRegion("eu-wales", "EU Wales", "$BASE/europe/great_britain/wales.map"),
        OsmRegion("eu-ireland", "EU Ireland", "$BASE/europe/ireland.map"),
        OsmRegion("eu-greece", "EU Greece", "$BASE/europe/greece.map"),
        OsmRegion("eu-sweden", "EU Sweden", "$BASE/europe/sweden.map"),
        OsmRegion("eu-norway", "EU Norway", "$BASE/europe/norway.map"),
        OsmRegion("eu-finland", "EU Finland", "$BASE/europe/finland.map"),
        OsmRegion("eu-denmark", "EU Denmark", "$BASE/europe/denmark.map"),
        OsmRegion("eu-ukraine", "EU Ukraine", "$BASE/europe/ukraine.map"),
        OsmRegion("eu-serbia", "EU Serbia", "$BASE/europe/serbia.map"),
        OsmRegion("eu-bulgaria", "EU Bulgaria", "$BASE/europe/bulgaria.map"),
        OsmRegion("asia-japan", "Japan", "$BASE/asia/japan.map"),
        OsmRegion("asia-india", "India", "$BASE/asia/india.map"),
        OsmRegion("us-california", "US California", "$BASE/north-america/united-states/california.map"),
        OsmRegion("us-new-york", "US New York", "$BASE/north-america/united-states/new-york.map"),
        OsmRegion("us-texas", "US Texas", "$BASE/north-america/united-states/texas.map"),
        OsmRegion("ca-ontario", "CA Ontario", "$BASE/north-america/canada/ontario.map"),
        OsmRegion("sa-brazil", "Brazil", "$BASE/south-america/brazil.map"),
        OsmRegion("au-australia", "Australia", "$BASE/australia-oceania/australia.map")
    )
}
