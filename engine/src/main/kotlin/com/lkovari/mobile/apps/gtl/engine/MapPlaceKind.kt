package com.lkovari.mobile.apps.gtl.engine

enum class MapPlaceKind {
    City,
    Town,
    Village,
    Hamlet,
    Suburb,
    Peak,
    Statue,
    Monument,
    Landmark,
    House,
    Building,
    Street,
    Place;

    fun zoom(): Int {
        val level = when (this) {
            City -> 11
            Town -> 12
            Village, Hamlet -> 14
            Suburb, Peak -> 15
            Street, Landmark, Monument -> 16
            House, Building, Statue, Place -> 17
        }
        return MapFitZoom.clamp(level)
    }
}
