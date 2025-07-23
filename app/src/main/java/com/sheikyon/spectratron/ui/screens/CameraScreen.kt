package com.sheikyon.spectratron.ui.screens

// Importaciones necesarias para el funcionamiento de la pantalla de la cámara.
// Cada 'import' trae una "herramienta" o "clase" específica que usaremos en el código.

// Importaciones de Android SDK y Jetpack (Componentes esenciales)
import androidx.lifecycle.lifecycleScope // Necesario para lanzar corrutinas vinculadas al ciclo de vida
import kotlinx.coroutines.launch // Para iniciar una corrutina
import kotlinx.coroutines.delay // Para la función de retraso
import android.Manifest // Para solicitar el permiso de acceso a la cámara.
import android.graphics.Bitmap // Clase para trabajar con imágenes de mapa de bits (pixels).
import android.graphics.Color // Clase para manipular colores (ej. convertir hexadecimal a int de color).
import android.util.Log // Para imprimir mensajes en el Logcat (útil para depuración).
import android.util.Size // Para especificar dimensiones (ancho y alto) de la imagen o vista.
import android.view.ViewGroup.LayoutParams.MATCH_PARENT // Constante para hacer que una vista ocupe todo el espacio disponible.
import android.widget.LinearLayout // Un tipo de layout básico de Android View (necesario para el PreviewView).

// Importaciones de CameraX (Librería de cámara de Jetpack)
import androidx.camera.core.CameraSelector // Para seleccionar qué cámara usar (frontal, trasera).
import androidx.camera.core.ImageAnalysis // Clase para analizar frames de video de la cámara.
import androidx.camera.core.ImageProxy // Representación de un frame de imagen capturado por la cámara.
import androidx.camera.core.Preview // Clase para configurar la vista previa de la cámara.
import androidx.camera.lifecycle.ProcessCameraProvider // Para obtener una instancia del proveedor de cámara y vincularla al ciclo de vida.
import androidx.camera.view.PreviewView // Una vista de Android diseñada para mostrar la vista previa de la cámara.

// Importaciones de Jetpack Compose (Para construir la UI)
import androidx.compose.foundation.Canvas // Composable para dibujar gráficos personalizados (círculos, líneas).
import androidx.compose.foundation.background // Modificador para aplicar un color de fondo a un Composable.
import androidx.compose.foundation.layout.* // Importa todos los modificadores de layout (Box, Column, Row, Spacer).
import androidx.compose.material3.Button // Composable para un botón con estilo Material Design.
import androidx.compose.material3.Text // Composable para mostrar texto.
import androidx.compose.runtime.* // Importa funciones clave de Compose como 'remember', 'mutableStateOf', 'LaunchedEffect', 'DisposableEffect'.
import androidx.compose.ui.Alignment // Para alinear Composable dentro de un contenedor.
import androidx.compose.ui.Modifier // Interfaz base para modificar el comportamiento o la apariencia de un Composable.
import androidx.compose.ui.geometry.Offset // Para representar coordenadas X, Y.
import androidx.compose.ui.graphics.drawscope.Stroke // Estilo de dibujo para solo el contorno (sin relleno).
import androidx.compose.ui.platform.LocalContext // Para obtener el contexto de Android en un Composable.
import androidx.compose.ui.platform.LocalLifecycleOwner // Para obtener el LifecycleOwner actual en un Composable.
import androidx.compose.ui.unit.dp // Unidad de medida para densidad de píxeles independientes.
import androidx.compose.ui.unit.sp // Unidad de medida para tamaño de texto (escala de píxeles).
import androidx.compose.ui.viewinterop.AndroidView // Composable para incrustar una vista tradicional de Android (View) dentro de Compose.
import androidx.core.content.ContextCompat // Utilidades para acceder a recursos y permisos de forma compatible con versiones anteriores de Android.
import androidx.lifecycle.ViewModel // Clase base para ViewModels de Android.
import androidx.lifecycle.viewmodel.compose.viewModel // Función de Compose para obtener/crear un ViewModel.

// Importaciones de Google Accompanist Permissions (Simplifica la gestión de permisos)
import com.google.accompanist.permissions.ExperimentalPermissionsApi // Anotación para indicar que se usan APIs experimentales de permisos.
import com.google.accompanist.permissions.isGranted // Extensión para verificar si un permiso ha sido concedido.
import com.google.accompanist.permissions.rememberPermissionState // Composable para recordar el estado de un permiso.
import com.google.accompanist.permissions.shouldShowRationale // Extensión para saber si se debe mostrar una explicación al usuario.

// Importaciones de Java (Funcionalidades estándar)
import java.io.ByteArrayOutputStream // Para escribir datos en un array de bytes.
import java.nio.ByteBuffer // Para trabajar con buffers de bytes (usado en ImageProxy).
import java.util.concurrent.Executors // Para crear y gestionar hilos de ejecución.

/**
 * [CameraViewModel]
 *
 * ViewModel para gestionar el estado de la pantalla de la cámara y la lógica de detección de color.
 *
 * POR QUÉ: Los ViewModels están diseñados para almacenar y gestionar datos relacionados con la interfaz de usuario
 * de una manera que sobrevive a los cambios de configuración (como rotaciones de pantalla).
 * Esto significa que cuando el usuario gira el dispositivo, la aplicación no pierde el color detectado
 * o el estado de bloqueo, y no tiene que reiniciar la cámara.
 * También ayuda a separar la lógica de negocio (cómo se procesa la imagen) de la UI (cómo se muestra).
 * CÓMO: Extiende [ViewModel] y utiliza [mutableStateOf] para hacer que las variables sean observables por Compose.
 * Cuando un valor de [mutableStateOf] cambia, Compose recompone automáticamente las partes de la UI que lo usan.
 */
class CameraViewModel : ViewModel() {
    // Guarda el color hexadecimal actualmente detectado por la cámara.
    // Inicialmente es blanco (#FFFFFF).
    val currentColorHex = mutableStateOf("#FFFFFF")

    // Indica si la detección de color está "bloqueada" (true) o activa (false).
    // Si está bloqueado, el color mostrado no cambia, incluso si la cámara se mueve.
    val isLocked = mutableStateOf(false)

    // Almacena un mensaje de error si ocurre algún problema al iniciar o usar la cámara.
    // Es nullable ('String?') porque inicialmente no hay errores (null).
    val cameraError = mutableStateOf<String?>(null)

    val canUpdateColor = mutableStateOf(true) // Semáforo para controlar las actualizaciones
}

/**
 * [CameraScreen]
 *
 * Función Composable principal que define la interfaz de usuario y la lógica para la pantalla de la cámara.
 *
 * POR QUÉ: Es el punto de entrada para mostrar todo lo relacionado con la cámara y su interacción en Compose.
 * CÓMO: Combina la vista previa de la cámara, el procesamiento de imágenes, la gestión de permisos
 * y la interfaz de usuario para mostrar el color detectado y los controles.
 *
 * @param viewModel Una instancia de [CameraViewModel] para acceder y modificar el estado de la pantalla.
 * Por defecto, usa [viewModel()] que crea o recupera una instancia gestionada por Compose/Lifecycle.
 */
@OptIn(ExperimentalPermissionsApi::class) // Anotación necesaria porque usamos APIs experimentales de Accompanist Permissions.
@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {
    // Proporciona el contexto de la aplicación, necesario para acceder a recursos del sistema
    // y servicios como el gestor de la cámara.
    val context = LocalContext.current

    // Proporciona el LifecycleOwner de la actividad o fragmento actual.
    // Es crucial para CameraX, ya que vincula el ciclo de vida de la cámara
    // al ciclo de vida de la UI, asegurando que la cámara se inicie y detenga automáticamente.
    val lifecycleOwner = LocalLifecycleOwner.current

    // Crea y recuerda el estado del permiso de la cámara.
    // Este objeto gestiona la lógica de si el permiso está concedido, denegado,
    // o si se debe mostrar una justificación al usuario para pedirlo.
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Crea y recuerda una instancia de PreviewView.
    // PreviewView es una vista de Android tradicional (no Compose) de CameraX
    // que muestra la alimentación en vivo de la cámara. La usamos en Compose a través de AndroidView.
    val previewView = remember { PreviewView(context) }

    /**
     * [startCamera]
     *
     * Función que encapsula toda la lógica para inicializar y configurar los casos de uso de CameraX:
     * vista previa y análisis de imagen.
     *
     * POR QUÉ: Centraliza la complejidad de la configuración de la cámara.
     * CÓMO: Se envuelve en [remember] para que esta función de configuración solo se prepare una vez
     * y no se recree innecesariamente cada vez que el Composable se recompone.
     */
    val startCamera = remember {
        {
            // Obtiene una instancia de ProcessCameraProvider, que se utiliza para vincular casos de uso de cámara
            // al ciclo de vida de la aplicación. Es una operación asíncrona, por eso devuelve un Future.
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            // Añade un listener para cuando el Future de cameraProvider esté listo.
            cameraProviderFuture.addListener({
                // Una vez que el cameraProvider está disponible, lo obtenemos del Future.
                val cameraProvider = cameraProviderFuture.get()

                // Configuración del caso de uso de Preview (la vista previa en pantalla).
                val preview = Preview.Builder().build().also {
                    // Vincula la salida de la vista previa al SurfaceProvider de nuestro PreviewView.
                    // Esto es lo que hace que la imagen de la cámara aparezca en el PreviewView.
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Configuración del caso de uso de ImageAnalysis (para procesar cada frame de la cámara).
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(previewView.width, previewView.height))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            // El análisis solo procede si el color NO está bloqueado por el usuario
                            // Y si actualmente podemos actualizar el color (es decir, no estamos en medio de un delay).
                            if (!viewModel.isLocked.value && viewModel.canUpdateColor.value) {
                                val hexColor = analyzeImageForColor(imageProxy)
                                if (hexColor != null) {
                                    ContextCompat.getMainExecutor(context).execute {
                                        viewModel.currentColorHex.value = hexColor
                                        // Una vez que actualizamos el color, marcamos que NO podemos actualizar de nuevo
                                        // hasta que el delay haya terminado.
                                        viewModel.canUpdateColor.value = false

                                        // Lanzamos una corrutina para gestionar el delay.
                                        // Esto no bloquea el hilo principal.
                                        lifecycleOwner.lifecycleScope.launch {
                                            delay(1000L) // Pausa la corrutina por 3 segundos.
                                            // Después del delay, permitimos que el color se actualice de nuevo.
                                            viewModel.canUpdateColor.value = true
                                        }
                                    }
                                }
                            }
                            // ¡IMPORTANTE! Siempre cierra el ImageProxy para liberar memoria.
                            imageProxy.close()
                        }
                    }

                // Selecciona la cámara trasera por defecto.
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Desvincula todos los casos de uso actuales de la cámara.
                    // Esto es importante para asegurar un estado limpio y evitar conflictos,
                    // especialmente si la Activity se recrea (ej., por una rotación de pantalla).
                    cameraProvider.unbindAll()

                    // Vincula los casos de uso (preview y imageAnalyzer) al ciclo de vida del componente (Activity/Fragment).
                    // POR QUÉ: CameraX gestionará automáticamente el inicio y detención de la cámara
                    //          en función del estado del ciclo de vida (resume, pause, destroy),
                    //          ahorrando batería y evitando que la cámara se quede activa en segundo plano.
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,    // El propietario del ciclo de vida (Activity/Fragment).
                        cameraSelector,    // La cámara seleccionada.
                        preview,           // El caso de uso para mostrar la vista previa.
                        imageAnalyzer      // El caso de uso para analizar las imágenes.
                    )
                    // Si la cámara se inicia correctamente, se borra cualquier mensaje de error anterior.
                    viewModel.cameraError.value = null
                } catch (exc: Exception) {
                    // Captura y registra cualquier excepción que ocurra durante la vinculación de la cámara.
                    Log.e("SpectraTron", "Fallo al vincular casos de uso de la cámara", exc)
                    // Muestra un mensaje de error legible al usuario a través del ViewModel.
                    viewModel.cameraError.value = "Error al iniciar cámara: ${exc.message}"
                }
            }, ContextCompat.getMainExecutor(context)) // El ejecutor para callbacks en el hilo principal.
        }
    }

    // --- Gestión de Permisos y Ciclo de Vida en Compose ---

    /**
     * [LaunchedEffect]
     *
     * Efecto secundario de Compose que se lanza una vez cuando el Composable entra en composición
     * (es decir, cuando la pantalla de la cámara se muestra por primera vez).
     *
     * POR QUÉ: Es el lugar ideal para iniciar operaciones asíncronas o de "lanzamiento"
     * que deben ocurrir una vez cuando la UI se carga, como solicitar permisos.
     * CÓMO: Se usa 'Unit' como clave para que se ejecute solo una vez.
     */
    LaunchedEffect(Unit) {
        // Comprueba si el permiso de la cámara NO está concedido.
        if (!cameraPermissionState.status.isGranted) {
            // Si no está concedido, solicita el permiso al usuario.
            cameraPermissionState.launchPermissionRequest()
        }
    }

    /**
     * [DisposableEffect]
     *
     * Efecto secundario de Compose que gestiona recursos que necesitan ser "limpiados"
     * cuando el Composable abandona la composición o cuando su clave cambia.
     *
     * POR QUÉ: Es perfecto para gestionar la cámara. La enciende cuando el permiso está
     * concedido y la pantalla es visible, y la apaga de forma segura cuando
     * la pantalla ya no está activa (ej., el usuario sale de la app, gira el dispositivo).
     * Esto previene fugas de memoria y asegura un uso eficiente de la batería.
     * CÓMO:
     * - La clave `key1 = cameraPermissionState.status.isGranted` significa que este efecto
     * se "reiniciará" (se llamará a `onDispose` y luego se volverá a ejecutar el bloque principal)
     * cada vez que el estado del permiso de la cámara cambie.
     * - El bloque `onDispose` contiene la lógica para liberar los recursos de la cámara.
     */
    DisposableEffect(key1 = cameraPermissionState.status.isGranted) {
        // Si el permiso de la cámara está concedido, entonces iniciamos la cámara.
        if (cameraPermissionState.status.isGranted) {
            startCamera()
        }
        // Este bloque se ejecuta cuando el Composable abandona la composición o cuando la clave cambia.
        onDispose {
            // Se obtiene una instancia del CameraProvider para apagar la cámara.
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                // Desvincula todos los casos de uso de la cámara para apagarla completamente.
                cameraProvider.unbindAll()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    // --- Interfaz de Usuario (UI) de SpectraTron ---

    /**
     * [Box]
     *
     * Un Composable que permite superponer elementos uno encima del otro.
     *
     * POR QUÉ: Es ideal para nuestra pantalla de cámara, ya que queremos que la vista previa
     * de la cámara esté en el fondo, y el punto de mira, los datos del color y los botones
     * estén flotando por encima.
     * CÓMO: [Modifier.fillMaxSize()] asegura que el Box ocupe toda la pantalla disponible.
     */
    Box(modifier = Modifier.fillMaxSize()) {
        // Comprueba si el permiso de la cámara ha sido concedido.
        if (cameraPermissionState.status.isGranted) {
            /**
             * [AndroidView]
             *
             * Un Composable que permite incrustar vistas tradicionales de Android (View) dentro de una jerarquía de Compose.
             *
             * POR QUÉ: CameraX proporciona su vista previa a través de [PreviewView], que es una View tradicional.
             * Para mostrarla en nuestra UI de Compose, necesitamos este "puente".
             * CÓMO:
             * - `modifier = Modifier.fillMaxSize()`: Asegura que la vista previa ocupe todo el espacio.
             * - `factory = { context -> ... }`: Es una lambda que se ejecuta la primera vez que se compone
             * y debe devolver la View tradicional de Android que queremos incrustar.
             */
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    previewView.apply {
                        // Establece los parámetros de diseño para que el PreviewView ocupe el ancho y alto completo.
                        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    }
                }
            )

            // Muestra un mensaje de error de la cámara si el ViewModel tiene uno.
            // `?.let {}` solo ejecuta el bloque si `viewModel.cameraError.value` no es null.
            viewModel.cameraError.value?.let { errorMessage ->
                /**
                 * [Text]
                 *
                 * Composable para mostrar texto en la pantalla.
                 */
                Text(
                    text = errorMessage, // El mensaje de error a mostrar.
                    color = androidx.compose.ui.graphics.Color.Red, // Color del texto (rojo).
                    fontSize = 18.sp, // Tamaño de la fuente.
                    modifier = Modifier
                        .align(Alignment.TopCenter) // Alinea el texto en la parte superior central del Box.
                        .padding(top = 16.dp) // Añade un padding superior.
                        // Fondo negro semi-transparente para que el texto sea legible sobre la imagen de la cámara.
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
                        .padding(8.dp) // Añade padding alrededor del texto dentro de su fondo.
                )
            }

            /**
             * [Canvas]
             *
             * Composable que proporciona un área para realizar dibujos gráficos personalizados.
             *
             * POR QUÉ: Es perfecto para dibujar el punto de mira central, ya que nos permite
             * dibujar formas geométricas directamente en la pantalla.
             * CÓMO: Dibuja un círculo blanco en el centro de la pantalla.
             */
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Calcula el centro exacto del área de dibujo del Canvas.
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.White, // Color del círculo (blanco).
                    radius = 8.dp.toPx(), // Radio del círculo (convertido de dp a píxeles).
                    center = center, // Centro de la pantalla.
                    // Dibuja solo el contorno del círculo, sin relleno.
                    style = Stroke(width = 2.dp.toPx()) // Grosor del contorno (convertido a píxeles).
                )
            }

            /**
             * [Column]
             *
             * Composable que organiza sus elementos hijos verticalmente.
             *
             * POR QUÉ: Es ideal para el panel de información del color y el botón,
             * ya que queremos apilarlos en la parte inferior de la pantalla.
             * CÓMO:
             * - `align(Alignment.BottomCenter)`: Coloca la columna en la parte inferior central del Box.
             * - `fillMaxWidth()`: Hace que la columna ocupe todo el ancho disponible.
             * - `background()`: Añade un fondo semi-transparente para mejorar la legibilidad.
             * - `padding()`: Añade espacio alrededor del contenido de la columna.
             * - `horizontalAlignment = Alignment.CenterHorizontally`: Centra los elementos hijos horizontalmente dentro de la columna.
             */
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                /**
                 * [Box]
                 *
                 * Composable simple que sirve como contenedor o para dibujar un fondo con color.
                 *
                 * POR QUÉ: Usado aquí para mostrar visualmente el color detectado como un cuadrado.
                 */
                Box(
                    modifier = Modifier
                        .size(60.dp) // Define el tamaño del cuadrado (60dp por 60dp).
                        // Establece el color de fondo del Box. Se convierte el string hexadecimal
                        // de currentColorHex (ej. "#FF0000") a un objeto Color de Compose.
                        .background(androidx.compose.ui.graphics.Color(Color.parseColor(viewModel.currentColorHex.value)))
                )
                /**
                 * [Spacer]
                 *
                 * Composable que crea un espacio vacío.
                 *
                 * POR QUÉ: Para añadir un pequeño espacio vertical entre el cuadrado de color y el texto.
                 */
                Spacer(modifier = Modifier.height(8.dp))

                /**
                 * [Text]
                 *
                 * Composable para mostrar el código hexadecimal del color.
                 */
                Text(
                    text = viewModel.currentColorHex.value, // El texto a mostrar es el valor hexadecimal.
                    color = androidx.compose.ui.graphics.Color.White, // El color del texto es blanco para contraste.
                    fontSize = 24.sp, // Tamaño de la fuente del texto.
                    modifier = Modifier.padding(bottom = 8.dp) // Padding inferior.
                )
                /**
                 * [Button]
                 *
                 * Composable para un botón interactivo.
                 *
                 * POR QUÉ: Permite al usuario "bloquear" o "desbloquear" la detección de color.
                 * CÓMO:
                 * - `onClick`: Define la acción que ocurre cuando se pulsa el botón.
                 * Aquí, se invierte el valor de `isLocked.value` (si es true, se vuelve false y viceversa).
                 * - El texto del botón cambia dinámicamente según el estado de `isLocked`.
                 */
                Button(onClick = { viewModel.isLocked.value = !viewModel.isLocked.value }) {
                    Text(if (viewModel.isLocked.value) "Desbloquear Color" else "Bloquear Color")
                }
            }

        } else {
            // --- Interfaz de Usuario para Permisos Denegados ---
            // Si el permiso de la cámara NO está concedido, se muestra un mensaje informativo
            // y un botón para que el usuario pueda solicitarlo.
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center, // Centra los elementos verticalmente.
                horizontalAlignment = Alignment.CenterHorizontally // Centra los elementos horizontalmente.
            ) {
                Text(
                    "SpectraTron necesita permiso de cámara para funcionar.",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // `shouldShowRationale` devuelve true si el usuario ha denegado el permiso previamente
                // y se recomienda mostrar una explicación adicional.
                if (cameraPermissionState.status.shouldShowRationale) {
                    Text(
                        "Por favor, concede el permiso de cámara para usar esta aplicación.",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                /**
                 * [Button]
                 *
                 * Botón para iniciar la solicitud del permiso de la cámara.
                 */
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Conceder Permiso de Cámara")
                }
            }
        }
    }
}

/**
 * [analyzeImageForColor]
 *
 * Función auxiliar para procesar un frame de imagen de la cámara ([ImageProxy])
 * y extraer el color RGB del píxel central, convirtiéndolo a formato hexadecimal.
 *
 * POR QUÉ: Esta es la lógica central de la funcionalidad de "colorímetro" de la app.
 * CÓMO:
 * 1. Extrae los planos YUV del [ImageProxy]. Las imágenes de la cámara a menudo están en formato YUV
 * por eficiencia (Y = luminancia, U y V = croma).
 * 2. Convierte los datos YUV a un formato NV21, que es un formato YUV común para Android.
 * 3. Crea un objeto [YuvImage] a partir de los datos NV21.
 * 4. Comprime el [YuvImage] a un formato JPEG en un [ByteArrayOutputStream].
 * 5. Decodifica el JPEG de los bytes a un [Bitmap] (un formato de imagen más fácil de manipular).
 * 6. Calcula las coordenadas del píxel central del [Bitmap].
 * 7. Obtiene el color del píxel central.
 * 8. Convierte el valor de color (entero) a una cadena hexadecimal (ej. "#RRGGBB").
 * 9. **Recicla el [Bitmap]** para liberar memoria.
 * 10. Maneja posibles excepciones durante el proceso.
 *
 * @param image El [ImageProxy] que representa un solo frame de imagen capturado por la cámara.
 * @return Una cadena hexadecimal del color (ej. "#FF0000" para rojo) o null si falla el procesamiento.
 *
 * NOTA DE RENDIMIENTO (PARA DESARROLLADORES AVANZADOS):
 * Este método de conversión YUV -> JPEG -> Bitmap -> Pixel es funcional y relativamente fácil de entender,
 * pero no es el más eficiente en términos de rendimiento. Para aplicaciones de muy alta demanda
 * o para procesar múltiples píxeles, un enfoque más optimizado sería acceder directamente
 * a los datos del plano YUV y realizar la conversión a RGB para el píxel deseado sin
 * la compresión a JPEG y la creación de un Bitmap completo. Sin embargo, para este caso de uso
 * de un solo píxel central, es suficiente y más sencillo de implementar.
 */
fun analyzeImageForColor(image: ImageProxy): String? {
    try {
        // Obtiene los planos de la imagen (Y, U, V para el formato YUV).
        val planes = image.planes
        val yBuffer: ByteBuffer = planes[0].buffer // Buffer para el plano de luminancia (Y).
        val uBuffer: ByteBuffer = planes[1].buffer // Buffer para el plano de croma (U).
        val vBuffer: ByteBuffer = planes[2].buffer // Buffer para el plano de croma (V).

        // Obtiene el tamaño de los datos de cada plano.
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        // Crea un array de bytes para almacenar los datos de imagen en formato NV21.
        // NV21 es un formato YUV en el que el plano Y va primero, seguido de los planos V y U entrelazados.
        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copia los datos de los buffers a nuestro array NV21.
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize) // V antes que U para NV21.
        uBuffer.get(nv21, ySize + vSize, uSize) // U después de V.

        // Crea un objeto YuvImage a partir de los datos NV21.
        val yuvImage = android.graphics.YuvImage(
            nv21,                                   // Datos de la imagen.
            android.graphics.ImageFormat.NV21,      // Formato de la imagen.
            image.width,                            // Ancho original de la imagen.
            image.height,                           // Alto original de la imagen.
            null                                    // Strides (se deja null para que lo calcule automáticamente).
        )

        // Prepara un flujo de salida para comprimir la imagen a JPEG.
        val out = ByteArrayOutputStream()
        // Comprime la YuvImage a un JPEG con una calidad del 100%.
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
        // Convierte el flujo de salida a un array de bytes que representa la imagen JPEG.
        val imageBytes = out.toByteArray()
        // Decodifica el array de bytes JPEG en un Bitmap de Android.
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // Verifica si el Bitmap se decodificó correctamente.
        if (bitmap == null) {
            Log.e("SpectraTron", "Failed to decode bitmap from image bytes.")
            return null
        }

        // Calcula las coordenadas del píxel central.
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2

        // Asegura que las coordenadas del centro estén dentro de los límites del Bitmap.
        if (centerX >= 0 && centerX < bitmap.width && centerY >= 0 && centerY < bitmap.height) {
            // Obtiene el valor del píxel en las coordenadas centrales.
            val pixel = bitmap.getPixel(centerX, centerY)
            // Convierte el valor entero del píxel a un string hexadecimal con formato "#RRGGBB".
            // (0xFFFFFF and pixel) se usa para asegurar que solo se consideren los 24 bits RGB.
            val hexColor = String.format("#%06X", (0xFFFFFF and pixel))
            // ¡IMPORTANTE!: Libera la memoria ocupada por el Bitmap.
            // Si no se recicla, puede llevar a problemas de memoria y crasheos.
            bitmap.recycle()
            return hexColor
        }
        // Si las coordenadas no son válidas (esto no debería pasar con un cálculo de centro estándar),
        // asegúrate de reciclar el bitmap de todas formas antes de salir.
        bitmap.recycle()
        return null
    } catch (e: Exception) {
        // Captura cualquier error que ocurra durante el procesamiento de la imagen.
        Log.e("SpectraTron", "Error processing image for color: ${e.message}", e)
        return null // Devuelve null si hay un error.
    }
    // La llamada a imageProxy.close() se realiza fuera de esta función,
    // en el bloque ImageAnalysis.setAnalyzer de CameraScreen.
}