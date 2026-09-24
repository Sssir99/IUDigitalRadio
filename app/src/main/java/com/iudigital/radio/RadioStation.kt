package com.iudigital.radio

import androidx.compose.ui.graphics.Color

/**
 * Modelo de datos de una emisora de radio.
 * Guarda el identificador, el nombre, la frecuencia
 * y un color distintivo para mostrarla en la lista.
 */
data class RadioStation(
    val id: Int,
    val nombre: String,
    val frecuencia: String,
    val color: Color
)

/**
 * Lista de emisoras disponibles en la aplicación.
 * Sirve como fuente de datos para la lista dinámica (LazyColumn).
 */
val listaEmisoras = listOf(
    RadioStation(1, "La X", "96.9 FM", Color(0xFFE91E63)),
    RadioStation(2, "Blu Radio", "89.9 FM", Color(0xFF2196F3)),
    RadioStation(3, "Radio Tiempo", "91.5 FM", Color(0xFF4CAF50)),
    RadioStation(4, "Caracol Radio", "810 AM", Color(0xFFFF9800)),
    RadioStation(5, "RCN Radio", "93.9 FM", Color(0xFF9C27B0))
)
