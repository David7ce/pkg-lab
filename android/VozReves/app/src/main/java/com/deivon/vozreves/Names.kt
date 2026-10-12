package com.deivon.vozreves

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Nombre de archivo por defecto: reverse_voice_2026_10_12_14-30-05.wav (voz del revés) o
 * normal_voice_… (sentido normal). Año_mes_día_hora-min-seg: sin ":" (no se permite en nombres de
 * archivo de Android) y con el día, para que dos grabaciones del mismo mes no se confundan.
 */
fun voiceFileName(time: Date, reverse: Boolean, ext: String = "wav"): String =
    "${if (reverse) "reverse" else "normal"}_voice_" +
        SimpleDateFormat("yyyy_MM_dd_HH-mm-ss", Locale.US).format(time) + ".$ext"
