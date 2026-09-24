package com.iudigital.radio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.iudigital.radio.ui.theme.IUDigitalRadioTheme
import java.io.ByteArrayOutputStream

/**
 * Saver personalizado para guardar un Bitmap dentro de rememberSaveable.
 * Convierte el Bitmap a un arreglo de bytes (PNG) para poder
 * serializarlo y restaurarlo tras una rotación de pantalla.
 */
val BitmapSaver = Saver<Bitmap?, ByteArray>(
    save = { bitmap ->
        if (bitmap == null) {
            // Sin foto: guardamos un arreglo vacío como marca de "nulo".
            ByteArray(0)
        } else {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
    },
    restore = { bytes ->
        if (bytes.isEmpty()) null else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
)

/**
 * Actividad principal. Solo monta el tema y la pantalla de radio.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IUDigitalRadioTheme {
                RadioApp()
            }
        }
    }
}

/**
 * Pantalla principal de la aplicación IU Digital Radio.
 * Estructura en tres secciones verticales:
 * 1. Perfil (foto + botón de cámara)
 * 2. Reproductor central (emisora actual + controles)
 * 3. Catálogo de emisoras (lista dinámica)
 */
@Composable
fun RadioApp() {
    val context = LocalContext.current

    // ------------------------- ESTADO DE LA APP -------------------------
    // rememberSaveable: preserva el valor ante rotaciones de pantalla.
    // Guardamos solo el id (Int) y no la emisora completa, porque el color
    // no es un tipo que rememberSaveable pueda guardar automáticamente.
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var selectedStationId by rememberSaveable { mutableStateOf(listaEmisoras.first().id) }

    // La foto usa rememberSaveable con un Saver personalizado que la
    // convierte a bytes (PNG), de modo que también sobrevive a las rotaciones.
    var foto by rememberSaveable(stateSaver = BitmapSaver) { mutableStateOf<Bitmap?>(null) }

    // Emisora actualmente seleccionada (se deriva del id guardado).
    val selectedStation = listaEmisoras.first { it.id == selectedStationId }

    // -------------------- LANZADORES DE CÁMARA Y PERMISO --------------------
    // Devuelve la foto como thumbnail (imagen pequeña en memoria).
    val tomarFotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) foto = bitmap
    }

    // Pide el permiso CAMERA en tiempo de ejecución.
    val permisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) tomarFotoLauncher.launch(null)
    }

    // Abre la cámara solo si ya hay permiso; si no, lo solicita primero.
    fun abrirCamara() {
        val yaConcedido = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (yaConcedido) {
            tomarFotoLauncher.launch(null)
        } else {
            permisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    // Genera una vibración corta (retroalimentación háptica).
    fun vibrar() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    // ------------------------- ESTRUCTURA DE LA UI -------------------------
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Sección 1: perfil del usuario
            SeccionPerfil(foto = foto, onTomarFoto = { abrirCamara() })

            Spacer(Modifier.height(16.dp))

            // Sección 2: reproductor central
            SeccionReproductor(
                station = selectedStation,
                isPlaying = isPlaying,
                isMuted = isMuted,
                onPlayPause = {
                    isPlaying = !isPlaying
                    vibrar()
                },
                onToggleMute = {
                    isMuted = !isMuted
                    vibrar()
                }
            )

            Spacer(Modifier.height(16.dp))

            // Sección 3: catálogo de emisoras
            Text(
                text = "Emisoras disponibles",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(8.dp))

            SeccionListaEmisoras(
                stations = listaEmisoras,
                selectedId = selectedStationId,
                onSeleccionar = { station ->
                    // Al elegir una emisora, se actualiza la selección
                    // y se marca como "reproduciendo".
                    selectedStationId = station.id
                    isPlaying = true
                    vibrar()
                    // Confirmación visual del cambio de emisora.
                    Toast.makeText(
                        context,
                        "Reproduciendo: ${station.nombre}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

/**
 * Sección superior: foto de perfil circular + botón de cámara.
 */
@Composable
fun SeccionPerfil(foto: Bitmap?, onTomarFoto: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Foto circular o placeholder con icono de persona
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (foto != null) {
                Image(
                    bitmap = foto.asImageBitmap(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Emoji como placeholder antes de capturar una foto
                Text("👤", fontSize = 32.sp)
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text("Usuario IU Digital", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Estudiante de IU Digital",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FilledIconButton(onClick = onTomarFoto) {
            Text("📷", fontSize = 22.sp)
        }
    }
}

/**
 * Sección central: tarjeta con la emisora actual y los controles
 * de reproducción (Play/Pause y Mute).
 */
@Composable
fun SeccionReproductor(
    station: RadioStation,
    isPlaying: Boolean,
    isMuted: Boolean,
    onPlayPause: () -> Unit,
    onToggleMute: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = station.color.copy(alpha = 0.15f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "Reproduciendo ahora",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(station.nombre, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(station.frecuencia, fontSize = 16.sp, color = station.color)
            Spacer(Modifier.height(16.dp))

            // Barras de ecualizador animadas cuando está reproduciendo.
            Ecualizador(activo = isPlaying, color = station.color)

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botón Play / Pause (alterna)
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(64.dp)
                ) {
                    Text(if (isPlaying) "⏸" else "▶", fontSize = 28.sp)
                }

                Spacer(Modifier.width(16.dp))

                // Botón Mute / Unmute (alterna)
                IconButton(onClick = onToggleMute) {
                    Text(if (isMuted) "🔇" else "🔊", fontSize = 22.sp)
                }
            }
        }
    }
}

/**
 * Barras de ecualizador animadas. Cuando la emisora está sonando,
 * las barras suben y bajan continuamente; en pausa quedan estáticas.
 */
@Composable
fun Ecualizador(activo: Boolean, color: Color) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(24.dp)
    ) {
        if (activo) {
            val transition = rememberInfiniteTransition(label = "ecualizador")
            // Cada barra usa una duración distinta para verse desincronizadas.
            val duraciones = listOf(320, 420, 260, 380)
            duraciones.forEach { duracion ->
                val altura by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(duracion, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "barra"
                )
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight(altura)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        } else {
            // En pausa, barras bajas y atenuadas.
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color.copy(alpha = 0.3f))
                )
            }
        }
    }
}

/**
 * Sección inferior: lista dinámica de emisoras con LazyColumn.
 * El elemento seleccionado se resalta con el color de la emisora.
 */
@Composable
fun SeccionListaEmisoras(
    stations: List<RadioStation>,
    selectedId: Int,
    onSeleccionar: (RadioStation) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(stations) { station ->
            val seleccionada = station.id == selectedId

            Card(
                onClick = { onSeleccionar(station) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (seleccionada) {
                        station.color.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                border = if (seleccionada) BorderStroke(2.dp, station.color) else null
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Punto de color identificativo de la emisora
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(station.color)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        station.nombre,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        station.frecuencia,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
