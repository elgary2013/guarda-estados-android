package com.guardaestados.data.settings

enum class IncludedHomeBackground(val storageKey: String) {
    AuraGreen("aura_green"),
    EmeraldWaves("emerald_waves"),
    LuminousNight("luminous_night");

    companion object {
        fun fromStorageKey(storageKey: String?): IncludedHomeBackground? {
            return entries.firstOrNull { background -> background.storageKey == storageKey }
        }
    }
}
