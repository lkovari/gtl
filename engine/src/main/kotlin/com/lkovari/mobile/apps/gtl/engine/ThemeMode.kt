package com.lkovari.mobile.apps.gtl.engine

enum class ThemeMode {
    AUTOMATIC,
    LIGHT,
    DARK;

    companion object {
        fun fromStored(raw: String?): ThemeMode {
            return entries.firstOrNull { it.name == raw } ?: AUTOMATIC
        }
    }
}
