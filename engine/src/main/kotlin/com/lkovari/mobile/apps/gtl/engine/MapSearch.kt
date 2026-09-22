package com.lkovari.mobile.apps.gtl.engine

import java.util.Locale

data class MapPlaceRecord(
    val displayName: String,
    val foldedAliases: List<String>,
    val kind: MapPlaceKind
)

data class MapSearchCandidate(
    val name: String,
    val nameFold: String,
    val kind: MapPlaceKind,
    val latitude: Double,
    val longitude: Double
)

data class MapSearchHit(
    val name: String,
    val kind: MapPlaceKind,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double
)

object MapSearch {
    const val MinQueryChars = 3
    const val ResultLimit = 5
    const val MaxIndexedPlaces = 250_000
    private const val CellDegrees = 0.00015

    private val whitespace = Regex("\\s+")
    private val nameKeys = listOf(
        "name",
        "name:hu",
        "name:en",
        "alt_name",
        "loc_name",
        "official_name"
    )
    private val roads = setOf(
        "motorway",
        "motorway_link",
        "trunk",
        "trunk_link",
        "primary",
        "primary_link",
        "secondary",
        "secondary_link",
        "tertiary",
        "tertiary_link",
        "unclassified",
        "residential",
        "living_street",
        "service",
        "pedestrian",
        "road"
    )

    fun mapKey(path: String, length: Long, lastModified: Long): String {
        return "$path|$length|$lastModified"
    }

    fun accepts(raw: String): Boolean {
        val trimmed = raw.trim()
        return trimmed.codePointCount(0, trimmed.length) >= MinQueryChars
    }

    fun fold(raw: String): String {
        val trimmed = raw.trim().lowercase(Locale.ROOT)
        val folded = buildString(trimmed.length) {
            for (ch in trimmed) {
                append(foldChar(ch))
            }
        }
        return whitespace.replace(folded, " ").trim()
    }

    fun sqlToken(raw: String): String? {
        if (!accepts(raw)) {
            return null
        }
        val tokens = fold(raw).split(' ').filter { it.isNotEmpty() }
        val best = tokens.maxByOrNull { it.length } ?: return null
        val clean = best.replace("%", "").replace("_", "")
        return clean.ifEmpty { null }
    }

    fun ftsMatch(raw: String): String? {
        val token = sqlToken(raw) ?: return null
        val clean = buildString(token.length) {
            for (ch in token) {
                if (ch.isLetterOrDigit()) {
                    append(ch)
                }
            }
        }
        if (clean.isEmpty()) {
            return null
        }
        return "\"$clean\"*"
    }

    fun cell(value: Double): Int {
        return kotlin.math.round(value / CellDegrees).toInt()
    }

    fun record(tags: Map<String, String>): MapPlaceRecord? {
        val kind = kindOf(tags) ?: return null
        val folded = LinkedHashSet<String>()
        for (key in nameKeys) {
            val raw = tags[key] ?: continue
            for (variant in nameVariants(raw)) {
                val alias = fold(variant)
                if (alias.length >= 2) {
                    folded.add(alias)
                }
            }
        }
        val street = tags["addr:street"]?.trim().orEmpty()
        val number = tags["addr:housenumber"]?.trim().orEmpty()
        if (street.isNotEmpty() && number.isNotEmpty()) {
            val alias = fold("$street $number")
            if (alias.length >= 2) {
                folded.add(alias)
            }
        }
        if (folded.isEmpty()) {
            return null
        }
        val display = displayName(tags, street, number) ?: return null
        if (fold(display).length < 2) {
            return null
        }
        return MapPlaceRecord(display, folded.toList(), kind)
    }

    fun rank(
        rawQuery: String,
        candidates: List<MapSearchCandidate>,
        originLatitude: Double,
        originLongitude: Double
    ): List<MapSearchHit> {
        if (!accepts(rawQuery)) {
            return emptyList()
        }
        val query = fold(rawQuery)
        val best = LinkedHashMap<String, Scored>()
        for (candidate in candidates) {
            val tier = tier(query, candidate.nameFold) ?: continue
            val distance = FixAcceptance.haversineMeters(
                originLatitude,
                originLongitude,
                candidate.latitude,
                candidate.longitude
            )
            val key = placeKey(candidate)
            val scored = Scored(candidate, tier, distance)
            val previous = best[key]
            if (previous == null || scored.beats(previous)) {
                best[key] = scored
            }
        }
        return best.values
            .sortedWith(compareBy<Scored> { it.tier }.thenBy { it.distance }.thenBy { it.candidate.name })
            .take(ResultLimit)
            .map { scored ->
                MapSearchHit(
                    name = scored.candidate.name,
                    kind = scored.candidate.kind,
                    latitude = scored.candidate.latitude,
                    longitude = scored.candidate.longitude,
                    distanceMeters = scored.distance
                )
            }
    }

    private fun placeKey(candidate: MapSearchCandidate): String {
        return "${candidate.kind}|${candidate.name}|${cell(candidate.latitude)}|${cell(candidate.longitude)}"
    }

    private fun tier(query: String, name: String): Int? {
        if (name == query) {
            return 0
        }
        val queryTokens = query.split(' ').filter { it.isNotEmpty() }
        val nameTokens = name.split(' ').filter { it.isNotEmpty() }
        if (queryTokens.isNotEmpty() && tokensInOrder(queryTokens, nameTokens)) {
            return 1
        }
        if (name.startsWith(query)) {
            return 2
        }
        if (queryTokens.isNotEmpty() && queryTokens.all { token -> name.contains(token) }) {
            return 3
        }
        return null
    }

    private fun tokensInOrder(query: List<String>, name: List<String>): Boolean {
        var start = 0
        for (token in query) {
            var found = -1
            for (index in start until name.size) {
                if (name[index].startsWith(token)) {
                    found = index
                    break
                }
            }
            if (found < 0) {
                return false
            }
            start = found + 1
        }
        return true
    }

    private fun kindOf(tags: Map<String, String>): MapPlaceKind? {
        when (tags["place"]) {
            "city" -> return MapPlaceKind.City
            "town" -> return MapPlaceKind.Town
            "village" -> return MapPlaceKind.Village
            "hamlet", "isolated_dwelling" -> return MapPlaceKind.Hamlet
            "suburb", "neighbourhood", "quarter", "borough", "locality", "allotments" -> return MapPlaceKind.Suburb
            null, "" -> Unit
            else -> return MapPlaceKind.Hamlet
        }
        if (isStatue(tags)) {
            return MapPlaceKind.Statue
        }
        when (tags["historic"]) {
            "memorial", "monument" -> return MapPlaceKind.Monument
            "castle", "ruins", "archaeological_site", "city_gate", "fort", "manor" -> return MapPlaceKind.Landmark
        }
        when (tags["tourism"]) {
            "attraction", "viewpoint", "museum", "alpine_hut", "wilderness_hut", "camp_site", "information", "gallery" ->
                return MapPlaceKind.Landmark
        }
        if (tags["amenity"] == "place_of_worship") {
            return MapPlaceKind.Landmark
        }
        if (tags["leisure"] == "park" || tags["leisure"] == "nature_reserve" || tags["boundary"] == "national_park") {
            return MapPlaceKind.Landmark
        }
        when (tags["natural"]) {
            "peak", "volcano", "saddle", "cave_entrance" -> return MapPlaceKind.Peak
        }
        if (tags["mountain_pass"] == "yes") {
            return MapPlaceKind.Peak
        }
        val highway = tags["highway"]
        if (highway != null && highway in roads) {
            return MapPlaceKind.Street
        }
        val named = hasName(tags)
        val addressed = !tags["addr:street"].isNullOrBlank() && !tags["addr:housenumber"].isNullOrBlank()
        if (tags.containsKey("building") && named) {
            return MapPlaceKind.Building
        }
        if (addressed) {
            return MapPlaceKind.House
        }
        if (named || highway != null) {
            return MapPlaceKind.Place
        }
        return null
    }

    private fun isStatue(tags: Map<String, String>): Boolean {
        return tags["artwork_type"] == "statue" ||
            tags["memorial"] == "statue" ||
            tags["historic"] == "statue" ||
            tags["tourism"] == "artwork"
    }

    private fun hasName(tags: Map<String, String>): Boolean {
        return nameKeys.any { key -> !tags[key].isNullOrBlank() }
    }

    private fun displayName(tags: Map<String, String>, street: String, number: String): String? {
        val base = tags["name"]?.let { raw -> nameVariants(raw).firstOrNull { it.isNotBlank() } }
        if (!base.isNullOrBlank()) {
            return base.trim()
        }
        for (key in listOf("name:hu", "name:en", "official_name", "loc_name", "alt_name")) {
            val value = tags[key]?.trim()
            if (!value.isNullOrEmpty()) {
                return nameVariants(value).firstOrNull { it.isNotBlank() }?.trim()
            }
        }
        if (street.isNotEmpty() && number.isNotEmpty()) {
            return "$street $number"
        }
        return null
    }

    private fun nameVariants(raw: String): List<String> {
        if (!raw.contains('\r')) {
            val trimmed = raw.trim()
            return if (trimmed.isEmpty()) emptyList() else listOf(trimmed)
        }
        val parts = raw.split('\r')
        val names = ArrayList<String>()
        val base = parts.first().trim()
        if (base.isNotEmpty()) {
            names.add(base)
        }
        for (part in parts.drop(1)) {
            val bits = part.split('\u0008')
            if (bits.size >= 2) {
                val name = bits[1].trim()
                if (name.isNotEmpty()) {
                    names.add(name)
                }
            }
        }
        return names
    }

    private fun foldChar(ch: Char): Char {
        return when (ch) {
            'á', 'à', 'ä', 'â', 'ã', 'å' -> 'a'
            'é', 'è', 'ë', 'ê' -> 'e'
            'í', 'ì', 'ï', 'î' -> 'i'
            'ó', 'ò', 'ö', 'ő', 'ô', 'õ' -> 'o'
            'ú', 'ù', 'ü', 'ű', 'û' -> 'u'
            'ý', 'ÿ' -> 'y'
            'ñ' -> 'n'
            'ç' -> 'c'
            else -> ch
        }
    }

    private data class Scored(
        val candidate: MapSearchCandidate,
        val tier: Int,
        val distance: Double
    ) {
        fun beats(other: Scored): Boolean {
            if (tier != other.tier) {
                return tier < other.tier
            }
            return distance < other.distance
        }
    }
}
