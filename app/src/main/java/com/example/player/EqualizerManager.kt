package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.os.Build

data class EqBand(
    val bandIndex: Int,
    val centerFreqHz: Int,
    val freqLabel: String,
    val minLevelMilliBel: Int,
    val maxLevelMilliBel: Int,
    val currentLevelMilliBel: Int
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val presetName: String = "Flat",
    val bands: List<EqBand> = emptyList(),
    val bassBoostStrength: Int = 0, // 0..1000
    val volumeBoostPercent: Int = 100, // 100% to 200%
    val reverbPreset: Short = PresetReverb.PRESET_NONE
)

class EqualizerManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null

    private var currentAudioSessionId: Int = 0
    private var state = EqualizerState()

    val presets = listOf("Flat", "Bass Heavy", "Rock", "Pop", "EDM", "Acoustic", "Movie Theater", "Vocal Booster", "Custom")

    fun getPresetNames(): List<String> = presets

    fun bindAudioSession(audioSessionId: Int): EqualizerState {
        if (audioSessionId == 0 || audioSessionId == currentAudioSessionId) {
            return getCurrentState()
        }
        currentAudioSessionId = audioSessionId

        release()

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = state.isEnabled
            }
        } catch (e: Exception) {
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = state.isEnabled
                setStrength(state.bassBoostStrength.toShort())
            }
        } catch (e: Exception) {
            bassBoost = null
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = state.volumeBoostPercent > 100
                    val targetGainMb = ((state.volumeBoostPercent - 100) * 20).coerceIn(0, 2000)
                    setTargetGain(targetGainMb)
                }
            }
        } catch (e: Exception) {
            loudnessEnhancer = null
        }

        try {
            presetReverb = PresetReverb(0, audioSessionId).apply {
                enabled = state.isEnabled && state.reverbPreset != PresetReverb.PRESET_NONE
                preset = state.reverbPreset
            }
        } catch (e: Exception) {
            presetReverb = null
        }

        return getCurrentState()
    }

    fun setEnabled(enabled: Boolean): EqualizerState {
        state = state.copy(isEnabled = enabled)
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            loudnessEnhancer?.enabled = enabled && state.volumeBoostPercent > 100
            presetReverb?.enabled = enabled && state.reverbPreset != PresetReverb.PRESET_NONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getCurrentState()
    }

    fun setBandLevel(bandIndex: Int, levelMilliBel: Int): EqualizerState {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), levelMilliBel.toShort())
            state = state.copy(presetName = "Custom")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getCurrentState()
    }

    fun applyPreset(presetName: String): EqualizerState {
        try {
            val eq = equalizer
            if (eq != null) {
                val numBands = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange[0].toInt()
                val maxLevel = eq.bandLevelRange[1].toInt()

                when (presetName) {
                    "Flat" -> {
                        for (i in 0 until numBands) {
                            eq.setBandLevel(i.toShort(), 0)
                        }
                    }
                    "Bass Heavy" -> {
                        for (i in 0 until numBands) {
                            val level = when (i) {
                                0 -> (maxLevel * 0.8f).toInt()
                                1 -> (maxLevel * 0.5f).toInt()
                                2 -> (maxLevel * 0.2f).toInt()
                                else -> 0
                            }
                            eq.setBandLevel(i.toShort(), level.toShort())
                        }
                    }
                    "Rock" -> {
                        for (i in 0 until numBands) {
                            val level = when (i) {
                                0 -> (maxLevel * 0.6f).toInt()
                                1 -> (maxLevel * 0.3f).toInt()
                                numBands - 2 -> (maxLevel * 0.4f).toInt()
                                numBands - 1 -> (maxLevel * 0.7f).toInt()
                                else -> -(maxLevel * 0.1f).toInt()
                            }
                            eq.setBandLevel(i.toShort(), level.toShort())
                        }
                    }
                    "Pop" -> {
                        for (i in 0 until numBands) {
                            val level = when (i) {
                                0 -> -(maxLevel * 0.1f).toInt()
                                1 -> (maxLevel * 0.3f).toInt()
                                2 -> (maxLevel * 0.5f).toInt()
                                3 -> (maxLevel * 0.3f).toInt()
                                else -> 0
                            }
                            eq.setBandLevel(i.toShort(), level.toShort())
                        }
                    }
                    "EDM" -> {
                        for (i in 0 until numBands) {
                            val level = when (i) {
                                0 -> (maxLevel * 0.9f).toInt()
                                1 -> (maxLevel * 0.6f).toInt()
                                numBands - 1 -> (maxLevel * 0.8f).toInt()
                                else -> 0
                            }
                            eq.setBandLevel(i.toShort(), level.toShort())
                        }
                    }
                    "Acoustic" -> {
                        for (i in 0 until numBands) {
                            val level = when (i) {
                                0 -> (maxLevel * 0.3f).toInt()
                                1 -> (maxLevel * 0.2f).toInt()
                                numBands - 1 -> (maxLevel * 0.4f).toInt()
                                else -> (maxLevel * 0.1f).toInt()
                            }
                            eq.setBandLevel(i.toShort(), level.toShort())
                        }
                    }
                    "Movie Theater" -> {
                        for (i in 0 until numBands) {
                            val level = when (i) {
                                0 -> (maxLevel * 0.5f).toInt()
                                1 -> (maxLevel * 0.2f).toInt()
                                numBands - 1 -> (maxLevel * 0.5f).toInt()
                                else -> (maxLevel * 0.2f).toInt()
                            }
                            eq.setBandLevel(i.toShort(), level.toShort())
                        }
                    }
                    "Vocal Booster" -> {
                        for (i in 0 until numBands) {
                            val level = when (i) {
                                0 -> -(maxLevel * 0.2f).toInt()
                                1 -> 0
                                2 -> (maxLevel * 0.6f).toInt()
                                3 -> (maxLevel * 0.5f).toInt()
                                else -> 0
                            }
                            eq.setBandLevel(i.toShort(), level.toShort())
                        }
                    }
                }
            }
            state = state.copy(presetName = presetName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getCurrentState()
    }

    fun setBassBoost(strength: Int): EqualizerState {
        try {
            val clamped = strength.coerceIn(0, 1000)
            bassBoost?.setStrength(clamped.toShort())
            state = state.copy(bassBoostStrength = clamped)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getCurrentState()
    }

    fun setVolumeBoost(percent: Int): EqualizerState {
        try {
            val clamped = percent.coerceIn(100, 200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer?.enabled = clamped > 100
                val gainMb = ((clamped - 100) * 20).coerceIn(0, 2000)
                loudnessEnhancer?.setTargetGain(gainMb)
            }
            state = state.copy(volumeBoostPercent = clamped)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getCurrentState()
    }

    fun setReverb(preset: Short): EqualizerState {
        try {
            presetReverb?.enabled = preset != PresetReverb.PRESET_NONE
            presetReverb?.preset = preset
            state = state.copy(reverbPreset = preset)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getCurrentState()
    }

    fun setReverbPreset(preset: Short): EqualizerState = setReverb(preset)

    fun getCurrentState(): EqualizerState {
        val bandsList = mutableListOf<EqBand>()
        val eq = equalizer
        if (eq != null) {
            try {
                val numBands = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange[0].toInt()
                val maxLevel = eq.bandLevelRange[1].toInt()

                for (i in 0 until numBands) {
                    val centerFreqMilliHz = eq.getCenterFreq(i.toShort())
                    val freqHz = centerFreqMilliHz / 1000
                    val label = if (freqHz >= 1000) "${freqHz / 1000} kHz" else "$freqHz Hz"
                    val currentLevel = eq.getBandLevel(i.toShort()).toInt()
                    bandsList.add(
                        EqBand(
                            bandIndex = i,
                            centerFreqHz = freqHz,
                            freqLabel = label,
                            minLevelMilliBel = minLevel,
                            maxLevelMilliBel = maxLevel,
                            currentLevelMilliBel = currentLevel
                        )
                    )
                }
            } catch (e: Exception) {
                createFallbackBands(bandsList)
            }
        } else {
            createFallbackBands(bandsList)
        }

        return state.copy(bands = bandsList)
    }

    private fun createFallbackBands(bandsList: MutableList<EqBand>) {
        val freqs = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
        freqs.forEachIndexed { index, label ->
            bandsList.add(
                EqBand(
                    bandIndex = index,
                    centerFreqHz = 60 * (index + 1),
                    freqLabel = label,
                    minLevelMilliBel = -1500,
                    maxLevelMilliBel = 1500,
                    currentLevelMilliBel = 0
                )
            )
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            loudnessEnhancer?.release()
            presetReverb?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            equalizer = null
            bassBoost = null
            loudnessEnhancer = null
            presetReverb = null
        }
    }
}
