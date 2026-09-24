# IU Digital Radio 📻

Aplicación móvil Android de radio, desarrollada como evidencia de aprendizaje.

## Descripción

**IU Digital Radio** es una app nativa Android construida con **Kotlin** y **Jetpack Compose** (maquetación declarativa, sin XML de vistas). Permite al usuario capturar una foto de perfil con la cámara del dispositivo, reproducir emisoras de radio con controles interactivos (Play, Pause, Mute) y seleccionar entre un catálogo de emisoras en tiempo real.

## Funcionalidades (MVP)

- Interfaz declarativa íntegra en Jetpack Compose (`Column`, `Row`, `Card`, `LazyColumn`, `Modifier`).
- Perfil de usuario con captura de fotografía en tiempo real mediante la cámara nativa (`ActivityResultContracts.TakePicturePreview`).
- Reproductor central con controles de audio (Play, Pause, Mute) y retroalimentación háptica (vibración).
- Lista dinámica de emisoras (`LazyColumn`) que actualiza la selección activa.
- Archivo instalable compilado en formato APK.

## Requerimientos técnicos

| RF | Descripción |
|---|---|
| RF-01 | Maquetación UI declarativa con Jetpack Compose |
| RF-02 | Perfil con captura de cámara |
| RF-03 | Permisos en tiempo de ejecución (`CAMERA`, `VIBRATE`) |
| RF-04 | Reproductor interactivo y estado dinámico (`mutableStateOf`, `rememberSaveable`) |
| RF-05 | Retroalimentación háptica (`Vibrator` / `VibratorManager`) |
| RF-06 | Lista dinámica de emisoras (`LazyColumn`) |
| RF-07 | Audio simulado con estados visuales |

## Estructura del proyecto

```
app/src/main/
├── AndroidManifest.xml        # Permisos CAMERA y VIBRATE
├── java/com/iudigital/radio/
│   ├── MainActivity.kt        # Pantalla principal (3 secciones) + estado e interactividad
│   └── RadioStation.kt        # Modelo de datos y catálogo de emisoras
└── res/                       # Recursos (strings, colores, tema, iconos)
```

## Arquitectura

La UI está organizada en tres composables verticales dentro de `RadioApp()`:

1. **Sección superior (Perfil)** — `SeccionPerfil`: foto circular + botón de cámara.
2. **Sección central (Reproductor)** — `SeccionReproductor`: tarjeta con la emisora actual y los controles.
3. **Sección inferior (Catálogo)** — `SeccionListaEmisoras`: lista de emisoras con `LazyColumn`.

El estado se gestiona con `mutableStateOf` y `rememberSaveable` (preserva los valores ante rotaciones de pantalla):

- `isPlaying` — si el audio está sonando.
- `isMuted` — si el audio está silenciado.
- `selectedStationId` — emisora seleccionada.

## Requisitos

- Android Studio (versión reciente).
- JDK (incluido en Android Studio como `jbr`).
- Minimum SDK: 24 (Android 7.0).

## Compilación

Generar el APK de depuración:

```bash
./gradlew :app:assembleDebug
```

El APK resultante se encuentra en:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Créditos

Proyecto académico — Evidencia de aprendizaje 3.

Trabajo individual (Opción B — Desarrollador Full-Stack Android).

Santiago Sanchez Salazar
