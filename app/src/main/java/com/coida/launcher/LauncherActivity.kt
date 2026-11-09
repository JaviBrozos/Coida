package com.coida.launcher

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.coida.BuildConfig
import com.coida.R
import com.coida.assistant.ActionCatalog
import com.coida.assistant.AssistantAction
import com.coida.assistant.AssistantController
import com.coida.assistant.cloud.AssistantConfig
import com.coida.assistant.cloud.OpenAiCompatibleClient
import com.coida.assistant.ui.AssistantScreen
import com.coida.core.ui.CoidaTheme
import com.coida.core.ui.NotHomeNoticeStage
import com.coida.data.AssistantSettings
import com.coida.data.ContactAction
import com.coida.data.ContactPhotos
import com.coida.data.ContactShortcut
import com.coida.data.ConfigRepository
import com.coida.data.DefaultFavorites
import com.coida.data.EMERGENCY_FAVORITE_ID
import com.coida.data.FavoriteApp
import com.coida.data.FavoritesStore
import com.coida.data.InstalledApp
import com.coida.data.InstallationId
import com.coida.data.InstalledAppsRepository
import com.coida.data.InterfaceSettings
import com.coida.data.LocalConfigRepository
import com.coida.data.PinStore
import com.coida.data.SettingsStore
import com.coida.data.ThemeMode
import com.coida.data.WallpaperStore
import com.coida.data.WHATSAPP_PACKAGE
import com.coida.settings.ADMIN_UNLOCK_MINUTES
import com.coida.settings.AdminScreen
import com.coida.settings.AppsScreen
import com.coida.settings.ArrangeScreen
import com.coida.settings.AssistantSettingsScreen
import com.coida.settings.BasicAppRow
import com.coida.settings.ContactsScreen
import com.coida.settings.InterfaceScreen
import com.coida.settings.OtherAppRow
import com.coida.settings.PinGate
import com.coida.settings.SettingsScreen
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pantallas de Coida. Son pocas y planas: no hace falta una librería de navegación. */
private enum class Screen {
    HOME, PIN, SETTINGS, INTERFACE, CONTACTS, APPS, ARRANGE, ADMIN, CHANGE_PIN, ASSISTANT, ASSISTANT_SETTINGS,
}

/**
 * Pantalla de inicio de Coida y todo lo que cuelga de ella.
 *
 * Está declarada en el manifiesto con las categorías HOME + DEFAULT, así que el
 * sistema la ofrece como lanzador. Aquí solo vive lo que es puramente de Android
 * (PackageManager, Intents, permisos, Toasts) y el estado que se guarda; todo lo
 * visual está en [HomeScreen] y en el paquete settings.
 */
class LauncherActivity : ComponentActivity() {

    private lateinit var repository: InstalledAppsRepository
    private lateinit var settingsStore: SettingsStore
    private lateinit var config: ConfigRepository
    private lateinit var pinStore: PinStore
    private lateinit var wallpaperStore: WallpaperStore
    private lateinit var contactPhotos: ContactPhotos

    private var screen by mutableStateOf(Screen.HOME)
    private var interfaceSettings by mutableStateOf(InterfaceSettings())
    private var homeApps by mutableStateOf(emptyList<FavoriteApp>())
    private var homeIds by mutableStateOf(emptyList<String>())
    private var contacts by mutableStateOf(emptyList<ContactShortcut>())
    private var adminUnlockedUntil by mutableLongStateOf(0L)
    private var whatsappInstalled by mutableStateOf(false)
    private var installedApps by mutableStateOf(emptyList<InstalledApp>())
    private var defaultPackages by mutableStateOf(emptySet<String>())

    /** Las fotos de la agenda de los contactos guardados, por número (D-57). Solo en memoria. */
    private var photos by mutableStateOf(emptyMap<String, ImageBitmap>())

    /** Hay permiso para leer esas fotos; si no, Ajustes → Contactos ofrece pedirlo. */
    private var photosAllowed by mutableStateOf(true)

    /** A dónde vuelve «Colocar las casillas»: a Ajustes, o a Aplicaciones si se llegó desde ahí. */
    private var arrangeReturn = Screen.SETTINGS

    /** La foto de fondo del inicio (D-47), ya leída del disco; null si no hay. */
    private var wallpaper by mutableStateOf<ImageBitmap?>(null)

    /** Hay foto, aunque aún no se haya leído: el inicio pinta sus franjas desde el principio. */
    private var hasWallpaper by mutableStateOf(false)

    /** La última foto elegida no se pudo usar: Interfaz lo dice hasta que se elija otra. */
    private var wallpaperFailed by mutableStateOf(false)

    /**
     * false si al pulsar Inicio se abre otra pantalla de inicio: el inicio y Ajustes
     * lo avisan arriba (D-48). De entrada true, para no enseñar el aviso antes de
     * saberlo.
     */
    private var isDefaultHome by mutableStateOf(true)

    /** El familiar cerró el aviso del inicio: solo estaba probando. Se guarda. */
    private var homeNoticeDismissed by mutableStateOf(false)

    /** Tras cerrarlo, el inicio explica dónde ponerlo más adelante, hasta otra equis. */
    private var homeNoticeExplaining by mutableStateOf(false)

    /**
     * El aviso de Ajustes, que vuelve a salir entero cada vez que se entra: allí sí
     * conviene recordarlo, aunque se cerrara en el inicio. null si ya se cerró.
     */
    private var settingsNotice by mutableStateOf<NotHomeNoticeStage?>(NotHomeNoticeStage.OFFER)

    /**
     * false hasta la primera carga de las casillas. Mientras, el inicio no se pinta:
     * si se pintara vacío, las casillas aparecerían de golpe y empujarían a los
     * contactos hacia abajo (D-43).
     */
    private var appsLoaded by mutableStateOf(false)

    /** La consulta al PackageManager en curso; una nueva cancela la anterior. */
    private var appsJob: Job? = null

    /** La lectura de las fotos de la agenda en curso; igual. */
    private var photosJob: Job? = null

    private val assistantConfig = AssistantConfig.fromBuildConfig()

    /**
     * null en la versión pública, que sale sin asistente (D-63): ni se crea, ni se
     * ve su círculo ni su fila de Ajustes. Así R8 puede quitar todo su código.
     */
    private var assistant: AssistantController? = null
    private var assistantSettings by mutableStateOf(AssistantSettings())
    private var assistantMicDenied by mutableStateOf(false)

    /** Casilla o contacto que el asistente ha señalado al guiar. Se quita al tocar algo. */
    private var highlightedId by mutableStateOf<String?>(null)

    /**
     * Aviso fijo de Ajustes → Contactos (alguien que ya estaba). No es un Toast:
     * nada desaparece solo. Se quita al tocar otra cosa en esa pantalla.
     */
    private var contactsNotice by mutableStateOf<String?>(null)

    /** Sube cada vez que se pulsa Inicio: la rejilla vuelve arriba, como estaba. */
    private var homeResets by mutableIntStateOf(0)

    /** A dónde vuelve el asistente al cerrarse: al inicio, o a sus ajustes si se estaba probando. */
    private var assistantReturn = Screen.HOME

    /**
     * true mientras esperamos volver de algo que hemos abierto nosotros (una app,
     * la tienda, el selector de contactos). Sirve para distinguir esa ida y vuelta
     * de que el usuario se marche por su cuenta, que cierra los ajustes.
     */
    private var salidaPropia = false

    private val pickContact =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) result.data?.data?.let(::addPickedContact)
        }

    // Llamar directo y ver las fotos de la agenda (D-57), juntos al añadir un
    // contacto, con el familiar delante. Si se deniegan no pasa nada: los contactos
    // de llamada abren el marcador y salen con su inicial.
    private val requestContactPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { reload() }

    // El selector de fotos de Android no pide permisos: Coida solo recibe la foto
    // elegida. Sale a pantalla aparte, como el de contactos.
    private val pickWallpaper =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            salidaPropia = false
            if (uri != null) saveWallpaper(uri)
        }

    // El diálogo de Android para elegir la pantalla de inicio (D-48). Al volver,
    // onResume ya mira si ahora lo es. Si dijo que no, los avisos pasan a explicar
    // dónde ponerlo más adelante, como si los hubiera cerrado.
    private val requestHomeRole =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            salidaPropia = false
            if (result.resultCode != RESULT_OK) {
                if (screen == Screen.HOME) closeHomeNotice() else settingsNotice = NotHomeNoticeStage.EXPLAIN
            }
        }

    // Se pide al encender el asistente, con el familiar delante (CU-15).
    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            assistantMicDenied = !granted
            if (granted) saveAssistantSettings(assistantSettings.copy(enabled = true))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A partir de Android 15 el dibujado a pantalla completa es obligatorio;
        // cada pantalla aparta su contenido de las barras.
        enableEdgeToEdge()

        repository = InstalledAppsRepository(this)
        settingsStore = SettingsStore(this)
        config = LocalConfigRepository(this)
        // Se crea ya, aunque hoy no se use: así cada instalación tiene el suyo desde
        // el primer arranque.
        InstallationId(this).get()
        pinStore = PinStore(this)
        wallpaperStore = WallpaperStore(this)
        contactPhotos = ContactPhotos(this)
        hasWallpaper = wallpaperStore.exists()
        loadWallpaper()
        if (BuildConfig.ASSISTANT_ENABLED) {
            val assistantClient = OpenAiCompatibleClient(assistantConfig)
            assistant = AssistantController(
                context = this,
                scope = lifecycleScope,
                speechToText = assistantClient,
                interpreter = assistantClient,
                onExecute = ::executeAssistantAction,
                onFinish = ::onAssistantFinished,
                isInForeground = { lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) },
            )
        }
        reload()

        setContent {
            val settings = interfaceSettings
            val dark = settings.theme == ThemeMode.DARK

            // El tema lo elige quien configura, no el sistema: los iconos de las
            // barras tienen que seguirle o saldrían blancos sobre blanco. Se vuelve a
            // aplicar si cambia la pantalla sin recrear la actividad (configChanges:
            // un plegable que se abre, otra densidad), porque Android lo restablece.
            LaunchedEffect(dark, LocalConfiguration.current) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(transparent)
                else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            // "Tamaño de la letra" se multiplica por la escala que ya tenga Android.
            //
            // Con suelo en 0,85, el de «Normal» (D-55): en la prueba con dos
            // personas mayores el tamaño de siempre les sobraba y pidieron un nivel
            // menos. Más abajo no: si Android también está en pequeño, la letra
            // de Coida no sigue encogiendo con él.
            //
            // Y con techo en 2: Android ya llega por sí solo al 200 %, y encima
            // "Enorme" daba 2,6. Con el techo, el ajuste de Coida sigue agrandando
            // a quien tiene Android en normal, pero nunca pasa del máximo del sistema.
            val density = LocalDensity.current
            val scaled = Density(
                density.density,
                (density.fontScale * settings.textSize.scale).coerceIn(MIN_TEXT_SCALE, MAX_TEXT_SCALE),
            )

            CompositionLocalProvider(LocalDensity provides scaled) {
                CoidaTheme(darkTheme = dark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        if (appsLoaded) CurrentScreen()
                    }
                }
            }
        }
    }

    /**
     * Se relee al volver a la pantalla de inicio: si mientras tanto se ha
     * instalado o desinstalado alguna app, las casillas se actualizan solas.
     */
    override fun onResume() {
        super.onResume()
        reload()
    }

    /** Pulsar Inicio desde cualquier sitio (también desde Ajustes) vuelve al inicio. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        assistant?.cancel()
        // Pulsar Inicio es «empezar de cero»: también quita la casilla señalada.
        highlightedId = null
        homeResets++
        screen = Screen.HOME
    }

    /**
     * Los ajustes se cierran al salir de Coida (RNF-SEG-02): si el familiar deja
     * el móvil a medias y luego se vuelve por recientes, hay que pasar otra vez
     * por el PIN.
     *
     * Se exceptúa la ida y vuelta que arranca el propio Coida —abrir la tienda
     * para instalar algo, el selector de contactos, los ajustes de Android—,
     * porque ahí el familiar sigue a lo suyo y echarle sería absurdo.
     */
    override fun onStop() {
        super.onStop()
        // Nadie vuelve a una pregunta a medias. La casilla señalada, en cambio, se
        // queda: si la pantalla se apaga mientras lo piensa, no debe desaparecer
        // sola. Se quita al tocar algo o al pulsar Inicio.
        assistant?.cancel()
        if (!salidaPropia && screen != Screen.HOME) screen = Screen.HOME
        salidaPropia = false
    }

    override fun onDestroy() {
        assistant?.release()
        super.onDestroy()
    }

    @Composable
    private fun CurrentScreen() {
        // Siempre activo, también en el inicio, donde ATRÁS no hace nada. Con el
        // atrás predictivo (activado de serie desde targetSdk 36), dejárselo al
        // sistema le haría animar un «volver al inicio» encima del propio inicio.
        BackHandler {
            when (screen) {
                Screen.HOME -> Unit
                Screen.ASSISTANT -> closeAssistant()
                else -> screen = parentOf(screen)
            }
        }

        // Con el asistente abierto la pantalla no se apaga: apagarla cancelaría la
        // pregunta, y quien lee despacio no puede tener un reloj en contra.
        val assistantOpen = screen == Screen.ASSISTANT
        DisposableEffect(assistantOpen) {
            if (assistantOpen) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }

        // Un fundido corto al cambiar de pantalla, en vez de un salto en seco: se
        // nota que algo ha cambiado sin que nada se desplace ni rebote. Con
        // «Quitar animaciones» de Android el fundido dura cero y es un corte, como
        // antes.
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                fadeIn(tween(SCREEN_FADE_IN_MS)) togetherWith fadeOut(tween(SCREEN_FADE_OUT_MS))
            },
            label = "pantalla",
        ) { target ->
            // Mientras se desvanece, la pantalla que sale sigue compuesta: que
            // TalkBack no pueda dejar el foco en ella y que un segundo toque por
            // temblor no caiga en un botón que ya se va.
            val leaving = transition.targetState == EnterExitState.PostExit
            Box(
                modifier = if (leaving) {
                    Modifier
                        .clearAndSetSemantics {}
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                }
                            }
                        }
                } else {
                    Modifier
                },
            ) {
                ScreenContent(target)
            }
        }
    }

    @Composable
    private fun ScreenContent(target: Screen) {
        if (target == Screen.HOME) {
            HomeScreen(
                apps = homeApps,
                contacts = homeContacts(),
                columns = interfaceSettings.columns,
                showClock = interfaceSettings.showClock,
                adminUnlockedUntil = adminUnlockedUntil,
                showAssistant = assistant != null && assistantSettings.enabled && assistantConfig.isConfigured,
                highlightedId = highlightedId,
                scrollResetKey = homeResets,
                contactsLayout = interfaceSettings.contactsLayout,
                wallpaper = wallpaper,
                hasWallpaper = hasWallpaper,
                notHomeNotice = when {
                    isDefaultHome -> null
                    homeNoticeExplaining -> NotHomeNoticeStage.EXPLAIN
                    homeNoticeDismissed -> null
                    else -> NotHomeNoticeStage.OFFER
                },
                onMakeHome = ::requestDefaultHome,
                onCloseNotHome = ::closeHomeNotice,
                onAppClick = {
                    highlightedId = null
                    openApp(it)
                },
                onContactClick = {
                    highlightedId = null
                    openContact(it)
                },
                onSettingsClick = {
                    highlightedId = null
                    screen = Screen.PIN
                },
                onAssistantClick = { openAssistant(from = Screen.HOME) },
            )
            return
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            when (target) {
                Screen.HOME -> Unit

                Screen.PIN -> PinGate(
                    hasPin = pinStore.hasPin(),
                    preparePin = pinStore::prepare,
                    checkPin = pinStore::check,
                    savePin = pinStore::save,
                    onUnlocked = {
                        settingsNotice = NotHomeNoticeStage.OFFER
                        screen = Screen.SETTINGS
                    },
                    onBack = { screen = Screen.HOME },
                    hasRecoveryQuestion = pinStore.hasRecoveryQuestion(),
                    recoveryQuestion = pinStore.recoveryQuestion(),
                    checkRecoveryAnswer = pinStore::checkRecoveryAnswer,
                    saveRecoveryQuestion = pinStore::saveRecoveryQuestion,
                    lockState = pinStore::lockState,
                    onExitToHome = { screen = Screen.HOME },
                )

                Screen.CHANGE_PIN -> PinGate(
                    hasPin = pinStore.hasPin(),
                    preparePin = pinStore::prepare,
                    checkPin = pinStore::check,
                    savePin = pinStore::save,
                    onUnlocked = { screen = Screen.SETTINGS },
                    onBack = { screen = Screen.SETTINGS },
                    changing = true,
                    hasRecoveryQuestion = pinStore.hasRecoveryQuestion(),
                    recoveryQuestion = pinStore.recoveryQuestion(),
                    checkRecoveryAnswer = pinStore::checkRecoveryAnswer,
                    saveRecoveryQuestion = pinStore::saveRecoveryQuestion,
                    lockState = pinStore::lockState,
                    onExitToHome = { screen = Screen.HOME },
                )

                Screen.SETTINGS -> SettingsScreen(
                    contactsOnHome = contacts.count { it.onHome },
                    adminActive = isAdminActive(),
                    assistant = assistantSettings,
                    showAssistant = assistant != null,
                    onBack = { screen = Screen.HOME },
                    onInterface = { screen = Screen.INTERFACE },
                    onContacts = { screen = Screen.CONTACTS },
                    onApps = { openAppsScreen() },
                    onArrange = { openArrange(from = Screen.SETTINGS) },
                    onAssistant = {
                        assistantMicDenied = false
                        screen = Screen.ASSISTANT_SETTINGS
                    },
                    onSoundAndDisplay = { startFirst(Intent(Settings.ACTION_SOUND_SETTINGS)) },
                    onAdmin = { screen = Screen.ADMIN },
                    onChangePin = { screen = Screen.CHANGE_PIN },
                    notHomeNotice = settingsNotice.takeUnless { isDefaultHome },
                    onMakeHome = ::requestDefaultHome,
                    onCloseNotHome = {
                        settingsNotice = if (settingsNotice == NotHomeNoticeStage.OFFER) NotHomeNoticeStage.EXPLAIN else null
                    },
                )

                Screen.INTERFACE -> InterfaceScreen(
                    settings = interfaceSettings,
                    onChange = {
                        interfaceSettings = it
                        config.saveInterface(it)
                    },
                    onBack = { screen = Screen.SETTINGS },
                    wallpaper = wallpaper,
                    wallpaperFailed = wallpaperFailed,
                    onPickWallpaper = ::launchWallpaperPicker,
                    onRemoveWallpaper = ::removeWallpaper,
                )

                Screen.CONTACTS -> ContactsScreen(
                    contacts = contacts,
                    whatsappInstalled = whatsappInstalled,
                    onAdd = {
                        contactsNotice = null
                        launchContactPicker()
                    },
                    onSetButton = ::setContactButton,
                    notice = contactsNotice,
                    onRemove = { person ->
                        contactsNotice = null
                        saveContacts(contacts.filterNot { it.id == person.id || it.isSamePersonAs(person) })
                    },
                    onBack = { contactsNotice = null; screen = Screen.SETTINGS },
                    photos = photos,
                    photosAllowed = photosAllowed,
                    onAllowPhotos = {
                        contactsNotice = null
                        requestContactPermissions.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                    },
                )

                Screen.APPS -> AppsScreen(
                    basics = DefaultFavorites.map {
                        BasicAppRow(
                            id = it.id,
                            label = getString(it.labelRes),
                            icon = it.iconRes,
                            onHome = it.id in homeIds,
                            note = if (it.id == EMERGENCY_FAVORITE_ID) getString(R.string.apps_emergency_note) else null,
                        )
                    },
                    others = installedApps
                        .filter { it.packageName !in defaultPackages }
                        .map { OtherAppRow(it, FavoritesStore.packageId(it.packageName) in homeIds) },
                    onToggle = { id, onHome ->
                        repository.setOnHome(id, onHome)
                        reload()
                    },
                    onBack = { screen = Screen.SETTINGS },
                    onArrange = { openArrange(from = Screen.APPS) },
                )

                Screen.ARRANGE -> ArrangeScreen(
                    apps = homeApps,
                    contacts = homeContacts(),
                    columns = interfaceSettings.columns,
                    onAppsOrder = ::saveAppOrder,
                    onContactsOrder = ::saveContactOrder,
                    onBack = { screen = arrangeReturn },
                )

                Screen.ADMIN -> AdminScreen(
                    unlockedUntil = adminUnlockedUntil,
                    onUnlock = {
                        saveAdminUnlock(System.currentTimeMillis() + ADMIN_UNLOCK_MINUTES * 60_000L)
                    },
                    onLock = { saveAdminUnlock(0L) },
                    onOpenStore = ::openStore,
                    onOpenAndroidSettings = { startFirst(Intent(Settings.ACTION_SETTINGS)) },
                    onChooseHome = {
                        startFirst(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
                    },
                    onBack = { screen = Screen.SETTINGS },
                )

                Screen.ASSISTANT -> assistant?.let { assistant ->
                    AssistantScreen(
                        state = assistant.state,
                        level = assistant.level,
                        onSpeakAgain = assistant::retry,
                        onDoneSpeaking = assistant::finishSpeaking,
                        onCancelAction = assistant::cancelAction,
                        onChoose = assistant::choose,
                        onNext = assistant::nextStep,
                        onRepeat = assistant::repeat,
                        onRetry = assistant::retry,
                        onFinish = assistant::finish,
                        onClose = ::closeAssistant,
                    )
                }

                Screen.ASSISTANT_SETTINGS -> AssistantSettingsScreen(
                    settings = assistantSettings,
                    configured = assistantConfig.isConfigured,
                    micDenied = assistantMicDenied,
                    onEnabledChange = ::setAssistantEnabled,
                    onModeChange = { saveAssistantSettings(assistantSettings.copy(mode = it)) },
                    onTry = { openAssistant(from = Screen.ASSISTANT_SETTINGS) },
                    onBack = { screen = Screen.SETTINGS },
                )
            }
        }
    }

    private fun parentOf(current: Screen): Screen = when (current) {
        Screen.HOME, Screen.PIN, Screen.SETTINGS -> Screen.HOME
        Screen.ASSISTANT -> assistantReturn
        Screen.ARRANGE -> arrangeReturn
        else -> Screen.SETTINGS
    }

    /** Los accesos directos del inicio, con lo que hace falta para pintarlos. */
    private fun homeContacts(): List<HomeContact> = contacts.filter { it.onHome }.map {
        HomeContact(
            shortcut = it,
            isAvailable = it.action != ContactAction.WHATSAPP || whatsappInstalled,
            photo = photos[it.number],
        )
    }

    private fun openArrange(from: Screen) {
        arrangeReturn = from
        screen = Screen.ARRANGE
    }

    /**
     * El familiar ha recolocado las casillas (D-59). Se aplica ya a lo que se ve,
     * para que nada salte mientras se vuelve a leer del PackageManager.
     */
    private fun saveAppOrder(ids: List<String>) {
        // Una carga que iba por detrás (al encender una app en Aplicaciones y venir
        // a colocarla) leyó el orden de antes y lo pondría encima del nuevo: se
        // cancela y se vuelve a leer, ya con el orden guardado.
        appsJob?.cancel()
        repository.saveHomeOrder(ids)
        val byId = homeApps.associateBy { it.id }
        homeApps = ids.mapNotNull { byId[it] } + homeApps.filterNot { it.id in ids }
        homeIds = repository.homeIds()
        loadApps(withInstalled = installedApps.isEmpty())
    }

    private fun saveContactOrder(ids: List<String>) {
        contacts = config.saveContactOrder(ids)
    }

    private fun reload() {
        val saved = config.load()
        interfaceSettings = saved.interfaceSettings
        assistantSettings = saved.assistant
        contacts = saved.contacts
        homeIds = saved.homeAppIds
        // Estado de este móvil, no configuración: se queda en SettingsStore.
        adminUnlockedUntil = settingsStore.adminUnlockedUntil()
        homeNoticeDismissed = settingsStore.homeNoticeDismissed()
        loadApps(withInstalled = screen == Screen.APPS)
        loadPhotos()
    }

    /**
     * Las fotos de la agenda (D-57), aparte de las casillas: leerlas no debe retrasar
     * el primer pintado del inicio. También es IPC, con la agenda, así que va en
     * `Dispatchers.IO`. `ContactPhotos` devuelve la misma foto si no ha cambiado.
     */
    private fun loadPhotos() {
        photosJob?.cancel()
        val numbers = contacts.map { it.number }
        photosJob = lifecycleScope.launch {
            val (allowed, loaded) = withContext(Dispatchers.IO) {
                contactPhotos.hasPermission() to contactPhotos.load(numbers)
            }
            photosAllowed = allowed
            if (loaded != photos) photos = loaded
        }
    }

    private fun openAppsScreen() {
        loadApps(withInstalled = true)
        screen = Screen.APPS
    }

    /**
     * Lo que pregunta al PackageManager, fuera del hilo principal: es IPC con el
     * sistema y carga un icono por casilla, y en el hilo principal arriesgaba un
     * «la aplicación no responde» (ANR) al volver al inicio. Lo que ya se ve se
     * queda hasta que llega lo nuevo, así que al volver nada parpadea.
     *
     * @param withInstalled también la lista de todas las apps, que solo hace falta
     *   en Ajustes → Aplicaciones.
     */
    private fun loadApps(withInstalled: Boolean) {
        appsJob?.cancel()
        appsJob = lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                LoadedApps(
                    home = repository.homeApps(),
                    whatsappInstalled = repository.isInstalled(WHATSAPP_PACKAGE),
                    isDefaultHome = resolvesHomeToCoida(),
                    installed = if (withInstalled) repository.installedApps() else null,
                    defaultPackages = if (withInstalled) repository.defaultFavoritePackages() else null,
                )
            }
            homeApps = loaded.home
            whatsappInstalled = loaded.whatsappInstalled
            isDefaultHome = loaded.isDefaultHome
            loaded.installed?.let { installedApps = it }
            loaded.defaultPackages?.let { defaultPackages = it }
            appsLoaded = true
        }
    }

    /** Resultado de una consulta de [loadApps], para aplicarlo de una vez. */
    private class LoadedApps(
        val home: List<FavoriteApp>,
        val whatsappInstalled: Boolean,
        val isDefaultHome: Boolean,
        val installed: List<InstalledApp>?,
        val defaultPackages: Set<String>?,
    )

    private fun isAdminActive() = adminUnlockedUntil > System.currentTimeMillis()

    private fun saveAdminUnlock(epochMillis: Long) {
        settingsStore.saveAdminUnlockedUntil(epochMillis)
        adminUnlockedUntil = epochMillis
    }

    private fun saveContacts(updated: List<ContactShortcut>) {
        config.saveContacts(updated)
        contacts = updated
    }

    // --- Abrir cosas ----------------------------------------------------------

    private fun openApp(app: FavoriteApp) {
        val intent = repository.launchIntentFor(app)
        if (intent == null) {
            showCannotOpen(app.label)
            return
        }
        try {
            startActivity(intent)
            salidaPropia = true
        } catch (_: ActivityNotFoundException) {
            // La app pudo desinstalarse entre que se pintó la casilla y el toque.
            showCannotOpen(app.label)
        }
    }

    private fun openContact(contact: ContactShortcut) {
        val intent = when (contact.action) {
            ContactAction.CALL -> callIntent(contact.number)
            ContactAction.WHATSAPP -> whatsAppIntent(contact.number)
        }
        startFirst(intent, label = contact.name)
    }

    /** Llamada directa si hay permiso; si no, el marcador con el número escrito. */
    private fun callIntent(number: String): Intent {
        val canCall = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        return Intent(
            if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL,
            Uri.fromParts("tel", number, null),
        )
    }

    // wa.me necesita el número internacional sin "+" ni espacios.
    private fun whatsAppIntent(number: String): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://wa.me/" + internationalDigits(number)),
    ).setPackage(WHATSAPP_PACKAGE)

    // --- Asistente de voz -------------------------------------------------------

    private fun openAssistant(from: Screen) {
        val assistant = assistant ?: return
        highlightedId = null
        assistantReturn = from
        screen = Screen.ASSISTANT
        assistant.start(
            mode = assistantSettings.mode,
            catalog = ActionCatalog(
                contacts = contacts,
                homeApps = homeApps,
                columns = interfaceSettings.columns,
                whatsappInstalled = whatsappInstalled,
                contactsLayout = interfaceSettings.contactsLayout,
            ),
        )
    }

    private fun closeAssistant() {
        assistant?.cancel()
        screen = assistantReturn
    }

    /** Al cerrar guiando hacia algo, se vuelve al inicio con ese botón marcado. */
    private fun onAssistantFinished(highlight: String?) {
        if (highlight != null) {
            highlightedId = highlight
            screen = Screen.HOME
        } else {
            screen = assistantReturn
        }
    }

    /**
     * El asistente ha dicho lo que va a hacer y nadie lo ha cancelado (D-56). Se
     * vuelve antes al inicio para que, al colgar o cerrar la app, sea lo que se
     * encuentre. El contacto y su número son los guardados en Coida, nunca algo que
     * haya dicho el modelo.
     */
    private fun executeAssistantAction(action: AssistantAction) {
        screen = Screen.HOME
        when (action) {
            is AssistantAction.CallContact ->
                startFirst(callIntent(action.contact.number), label = action.contact.name)

            is AssistantAction.WhatsAppContact ->
                startFirst(whatsAppIntent(action.contact.number), label = action.contact.name)

            is AssistantAction.OpenApp -> openApp(action.app)
        }
    }

    private fun setAssistantEnabled(enabled: Boolean) {
        when {
            !enabled -> saveAssistantSettings(assistantSettings.copy(enabled = false))

            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> {
                assistantMicDenied = false
                saveAssistantSettings(assistantSettings.copy(enabled = true))
            }

            else -> requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun saveAssistantSettings(settings: AssistantSettings) {
        assistantSettings = settings
        config.saveAssistant(settings)
    }

    /**
     * Pasa un número a formato internacional con el país del móvil (SIM, luego
     * red, luego idioma), para que "600 123 456" funcione igual que "+34600123456".
     */
    private fun internationalDigits(number: String): String {
        val telephony = getSystemService(TelephonyManager::class.java)
        val country = listOfNotNull(
            telephony?.simCountryIso,
            telephony?.networkCountryIso,
            Locale.getDefault().country,
        ).firstOrNull { it.isNotBlank() }?.uppercase(Locale.ROOT)
        val e164 = country?.let { PhoneNumberUtils.formatNumberToE164(number, it) } ?: number
        return e164.filter { it.isDigit() }
    }

    private fun launchContactPicker() {
        try {
            pickContact.launch(
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
            )
            salidaPropia = true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_cannot_open_generic, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Enciende o apaga el botón de llamada o de WhatsApp de una persona (D-40). Si
     * ese botón ya existe, solo cambia si sale en el inicio, y conserva su sitio; si
     * no, se crea al final, para no mover las casillas que ya hay.
     */
    private fun setContactButton(person: ContactShortcut, action: ContactAction, onHome: Boolean) {
        contactsNotice = null
        val existing = contacts.firstOrNull {
            (it.id == person.id || it.isSamePersonAs(person)) && it.action == action
        }
        when {
            existing != null ->
                saveContacts(contacts.map { if (it.id == existing.id) it.copy(onHome = onHome) else it })

            onHome -> saveContacts(
                contacts + person.copy(id = UUID.randomUUID().toString(), action = action, onHome = true),
            )
        }
    }

    /**
     * El selector de contactos concede permiso de lectura solo sobre la fila
     * elegida: el nombre y el número salen sin pedir acceso a la agenda. La foto sí
     * lo pide (D-57), y solo se usa para los números guardados aquí.
     */
    private fun addPickedContact(uri: Uri) {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        val picked: Pair<String?, String?>? = runCatching {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) to cursor.getString(1) else null
            }
        }.getOrNull()

        val number = picked?.second
        if (number.isNullOrBlank()) {
            Toast.makeText(this, R.string.error_contact_pick, Toast.LENGTH_LONG).show()
            return
        }

        val shortcut = ContactShortcut(
            id = UUID.randomUUID().toString(),
            name = picked.first?.takeIf { it.isNotBlank() } ?: number,
            number = number,
            action = ContactAction.CALL,
            onHome = true,
        )
        // Ya tiene tarjeta: sus dos botones se encienden desde ahí (D-40).
        if (contacts.any { it.isSamePersonAs(shortcut) }) {
            contactsNotice = getString(R.string.contacts_already_added, shortcut.name)
            return
        }
        saveContacts(contacts + shortcut)

        // Se piden aquí, con el familiar delante, y no en el inicio con la persona mayor.
        val missing = listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestContactPermissions.launch(missing.toTypedArray())
    }

    private fun openStore() {
        val store = packageManager.getLaunchIntentForPackage(PLAY_STORE_PACKAGE)
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps"))
        startFirst(*listOfNotNull(store, web).toTypedArray())
    }

    // --- Pantalla de inicio y foto de fondo --------------------------------------

    /**
     * Si al pulsar Inicio se abriría Coida. En Android 9 o antes, sin elegir aún,
     * Android contesta con su selector, que no es Coida, y se avisa. Si la consulta falla, se da por buena:
     * mejor sin aviso que con uno falso. Es una consulta al PackageManager, así
     * que va dentro de [loadApps], fuera del hilo principal.
     */
    private fun resolvesHomeToCoida(): Boolean {
        // Desde Android 10 se le pregunta al sistema quién tiene el papel de inicio.
        // Preguntar al PackageManager no vale: desde Android 11 Coida no ve los
        // otros launchers y la respuesta salía falsa.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roles.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = runCatching {
            packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrNull() ?: return true
        return resolved.activityInfo?.packageName == packageName
    }

    /**
     * Desde Android 10, el diálogo del sistema que pregunta si Coida pasa a ser la
     * pantalla de inicio: no saca a otra parte del móvil. Antes, o si el móvil no
     * lo tiene, los ajustes de Android donde se elige, pero solo desde Ajustes.
     */
    private fun requestDefaultHome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                try {
                    salidaPropia = true
                    requestHomeRole.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    return
                } catch (_: ActivityNotFoundException) {
                    salidaPropia = false
                }
            }
        }
        // Sin el diálogo, el camino son los ajustes de Android, y a esos solo se llega
        // tras el PIN (invariante 5): desde el inicio, el botón lleva a Ajustes de
        // Coida, donde el mismo aviso abre los de Android.
        if (screen == Screen.HOME) {
            screen = Screen.PIN
            return
        }
        startFirst(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    }

    /** La equis del aviso del inicio: primero explica dónde ponerlo, luego se va. */
    private fun closeHomeNotice() {
        if (!homeNoticeDismissed) {
            homeNoticeDismissed = true
            homeNoticeExplaining = true
            settingsStore.saveHomeNoticeDismissed()
        } else {
            homeNoticeExplaining = false
        }
    }

    private fun loadWallpaper() {
        lifecycleScope.launch {
            wallpaper = withContext(Dispatchers.IO) { wallpaperStore.load() }
            // Si estaba pero no se puede leer, el inicio sale sin foto ni franjas.
            hasWallpaper = wallpaper != null
        }
    }

    private fun launchWallpaperPicker() {
        wallpaperFailed = false
        try {
            salidaPropia = true
            pickWallpaper.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: ActivityNotFoundException) {
            salidaPropia = false
            wallpaperFailed = true
        }
    }

    /** Copia la foto reducida al tamaño de la pantalla, fuera del hilo principal. */
    private fun saveWallpaper(uri: Uri) {
        val metrics = resources.displayMetrics
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                if (wallpaperStore.save(uri, metrics.widthPixels, metrics.heightPixels)) wallpaperStore.load() else null
            }
            wallpaperFailed = loaded == null
            if (loaded != null) {
                wallpaper = loaded
                hasWallpaper = true
            }
        }
    }

    private fun removeWallpaper() {
        wallpaperFailed = false
        wallpaper = null
        hasWallpaper = false
        lifecycleScope.launch(Dispatchers.IO) { wallpaperStore.clear() }
    }

    /** Prueba las intenciones en orden y se queda con la primera que funcione. */
    private fun startFirst(vararg intents: Intent, label: String? = null) {
        for (intent in intents) {
            try {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                salidaPropia = true
                return
            } catch (_: ActivityNotFoundException) {
                // Siguiente.
            } catch (_: SecurityException) {
                // Siguiente.
            }
        }
        if (label != null) showCannotOpen(label)
        else Toast.makeText(this, R.string.error_cannot_open_generic, Toast.LENGTH_LONG).show()
    }

    private fun showCannotOpen(label: String) {
        Toast.makeText(
            this,
            getString(R.string.error_cannot_open, label),
            Toast.LENGTH_LONG,
        ).show()
    }

    private companion object {
        const val PLAY_STORE_PACKAGE = "com.android.vending"

        /** Escala de letra máxima: la misma que Android permite por sí solo (200 %). */
        const val MAX_TEXT_SCALE = 2f

        /** Y mínima: la de «Normal» con Android en normal (D-55). */
        const val MIN_TEXT_SCALE = 0.85f

        /** El fundido entre pantallas: la nueva entra algo más despacio de lo que sale la vieja. */
        const val SCREEN_FADE_IN_MS = 180
        const val SCREEN_FADE_OUT_MS = 120
    }
}
