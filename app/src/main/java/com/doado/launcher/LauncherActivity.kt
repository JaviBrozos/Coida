package com.doado.launcher

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.doado.R
import com.doado.assistant.ActionCatalog
import com.doado.assistant.AssistantAction
import com.doado.assistant.AssistantController
import com.doado.assistant.cloud.AssistantConfig
import com.doado.assistant.cloud.OpenAiCompatibleClient
import com.doado.assistant.ui.AssistantScreen
import com.doado.core.ui.DoadoTheme
import com.doado.core.ui.NotHomeNoticeStage
import com.doado.data.AssistantSettings
import com.doado.data.ContactAction
import com.doado.data.ContactShortcut
import com.doado.data.ContactShortcutsStore
import com.doado.data.DefaultFavorites
import com.doado.data.EMERGENCY_FAVORITE_ID
import com.doado.data.FavoriteApp
import com.doado.data.FavoritesStore
import com.doado.data.InstalledApp
import com.doado.data.InstalledAppsRepository
import com.doado.data.InterfaceSettings
import com.doado.data.PinStore
import com.doado.data.SettingsStore
import com.doado.data.ThemeMode
import com.doado.data.WallpaperStore
import com.doado.data.WHATSAPP_PACKAGE
import com.doado.settings.ADMIN_UNLOCK_MINUTES
import com.doado.settings.AdminScreen
import com.doado.settings.AppsScreen
import com.doado.settings.AssistantSettingsScreen
import com.doado.settings.BasicAppRow
import com.doado.settings.ContactsScreen
import com.doado.settings.InterfaceScreen
import com.doado.settings.OtherAppRow
import com.doado.settings.PinGate
import com.doado.settings.SettingsScreen
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pantallas de Doado. Son pocas y planas: no hace falta una librería de navegación. */
private enum class Screen {
    HOME, PIN, SETTINGS, INTERFACE, CONTACTS, APPS, ADMIN, CHANGE_PIN, ASSISTANT, ASSISTANT_SETTINGS,
}

/**
 * Pantalla de inicio de Doado y todo lo que cuelga de ella.
 *
 * Está declarada en el manifiesto con las categorías HOME + DEFAULT, así que el
 * sistema la ofrece como lanzador. Aquí solo vive lo que es puramente de Android
 * (PackageManager, Intents, permisos, Toasts) y el estado que se guarda; todo lo
 * visual está en [HomeScreen] y en el paquete settings.
 */
class LauncherActivity : ComponentActivity() {

    private lateinit var repository: InstalledAppsRepository
    private lateinit var settingsStore: SettingsStore
    private lateinit var contactsStore: ContactShortcutsStore
    private lateinit var pinStore: PinStore
    private lateinit var wallpaperStore: WallpaperStore

    private var screen by mutableStateOf(Screen.HOME)
    private var interfaceSettings by mutableStateOf(InterfaceSettings())
    private var homeApps by mutableStateOf(emptyList<FavoriteApp>())
    private var homeIds by mutableStateOf(emptyList<String>())
    private var contacts by mutableStateOf(emptyList<ContactShortcut>())
    private var adminUnlockedUntil by mutableLongStateOf(0L)
    private var whatsappInstalled by mutableStateOf(false)
    private var installedApps by mutableStateOf(emptyList<InstalledApp>())
    private var defaultPackages by mutableStateOf(emptySet<String>())

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

    private val assistantConfig = AssistantConfig.fromBuildConfig()
    private lateinit var assistant: AssistantController
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

    // Si se deniega no pasa nada: los contactos de llamada abren el marcador.
    private val requestCallPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // El selector de fotos de Android no pide permisos: Doado solo recibe la foto
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
        contactsStore = ContactShortcutsStore(this)
        pinStore = PinStore(this)
        wallpaperStore = WallpaperStore(this)
        hasWallpaper = wallpaperStore.exists()
        loadWallpaper()
        val assistantClient = OpenAiCompatibleClient(assistantConfig)
        assistant = AssistantController(
            context = this,
            scope = lifecycleScope,
            speechToText = assistantClient,
            interpreter = assistantClient,
            onExecute = ::executeAssistantAction,
            onFinish = ::onAssistantFinished,
        )
        reload()

        setContent {
            val settings = interfaceSettings
            val dark = settings.theme == ThemeMode.DARK

            // El tema lo elige quien configura, no el sistema: los iconos de las
            // barras tienen que seguirle o saldrían blancos sobre blanco.
            LaunchedEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(transparent)
                else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            // "Tamaño de la letra" se multiplica por la escala que ya tenga Android.
            //
            // Con suelo en 1: Android deja bajar su escala hasta 0,85, y sin este
            // tope toda la tipografía de Doado encogía con ella. "No disponible"
            // se quedaba en 15,3 sp, por debajo del mínimo de 18 sp del proyecto.
            // Doado puede agrandar el texto respecto al sistema, nunca encogerlo.
            //
            // Y con techo en 2: Android ya llega por sí solo al 200 %, y encima
            // "Muy grande" daba 2,6. A esa escala el inicio se quedaba con una
            // franja de rejilla y los nombres partidos a media palabra
            // ("Cáma / ra"). Con el techo, el ajuste de Doado sigue agrandando a
            // quien tiene Android en normal, pero nunca pasa del máximo del sistema.
            val density = LocalDensity.current
            val scaled = Density(
                density.density,
                (density.fontScale * settings.textSize.scale).coerceIn(1f, MAX_TEXT_SCALE),
            )

            CompositionLocalProvider(LocalDensity provides scaled) {
                DoadoTheme(darkTheme = dark) {
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
        assistant.cancel()
        // Pulsar Inicio es «empezar de cero»: también quita la casilla señalada.
        highlightedId = null
        homeResets++
        screen = Screen.HOME
    }

    /**
     * Los ajustes se cierran al salir de Doado (RNF-SEG-02): si el familiar deja
     * el móvil a medias y luego se vuelve por recientes, hay que pasar otra vez
     * por el PIN.
     *
     * Se exceptúa la ida y vuelta que arranca el propio Doado —abrir la tienda
     * para instalar algo, el selector de contactos, los ajustes de Android—,
     * porque ahí el familiar sigue a lo suyo y echarle sería absurdo.
     */
    override fun onStop() {
        super.onStop()
        // Nadie vuelve a una pregunta a medias. La casilla señalada, en cambio, se
        // queda: si la pantalla se apaga mientras lo piensa, no debe desaparecer
        // sola. Se quita al tocar algo o al pulsar Inicio.
        assistant.cancel()
        if (!salidaPropia && screen != Screen.HOME) screen = Screen.HOME
        salidaPropia = false
    }

    override fun onDestroy() {
        assistant.release()
        super.onDestroy()
    }

    @Composable
    private fun CurrentScreen() {
        BackHandler(enabled = screen != Screen.HOME) {
            if (screen == Screen.ASSISTANT) closeAssistant() else screen = parentOf(screen)
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
                contacts = contacts.filter { it.onHome }.map {
                    HomeContact(
                        shortcut = it,
                        isAvailable = it.action != ContactAction.WHATSAPP ||
                            whatsappInstalled,
                    )
                },
                columns = interfaceSettings.columns,
                showClock = interfaceSettings.showClock,
                adminUnlockedUntil = adminUnlockedUntil,
                showAssistant = assistantSettings.enabled && assistantConfig.isConfigured,
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
                    onBack = { screen = Screen.HOME },
                    onInterface = { screen = Screen.INTERFACE },
                    onContacts = { screen = Screen.CONTACTS },
                    onApps = { openAppsScreen() },
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
                        settingsStore.saveInterfaceSettings(it)
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

                Screen.ASSISTANT -> AssistantScreen(
                    state = assistant.state,
                    level = assistant.level,
                    followUpListening = assistant.followUpListening,
                    onSpeakAgain = assistant::speakAgain,
                    onDoneSpeaking = assistant::finishSpeaking,
                    onConfirm = assistant::confirm,
                    onDecline = assistant::decline,
                    onChoose = assistant::choose,
                    onNext = assistant::nextStep,
                    onRepeat = assistant::repeat,
                    onRetry = assistant::retry,
                    onFinish = assistant::finish,
                    onClose = ::closeAssistant,
                )

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
        else -> Screen.SETTINGS
    }

    private fun reload() {
        interfaceSettings = settingsStore.interfaceSettings()
        assistantSettings = settingsStore.assistantSettings()
        adminUnlockedUntil = settingsStore.adminUnlockedUntil()
        homeNoticeDismissed = settingsStore.homeNoticeDismissed()
        contacts = contactsStore.all()
        homeIds = repository.homeIds()
        loadApps(withInstalled = screen == Screen.APPS)
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
                    isDefaultHome = resolvesHomeToDoado(),
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
        contactsStore.save(updated)
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
        assistant.cancel()
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
     * La persona mayor ha dicho «Sí» (D-26). Se vuelve antes al inicio para que, al
     * colgar o cerrar la app, sea lo que se encuentre. El contacto y su número son
     * los guardados en Doado, nunca algo que haya dicho el modelo.
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
        settingsStore.saveAssistantSettings(settings)
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
     * El selector de contactos concede permiso de lectura solo sobre la fila
     * elegida, así que Doado no necesita pedir acceso a toda la agenda.
     */
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

        // Se pide aquí, con el familiar delante, y no en el inicio con la persona mayor.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestCallPermission.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun openStore() {
        val store = packageManager.getLaunchIntentForPackage(PLAY_STORE_PACKAGE)
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps"))
        startFirst(*listOfNotNull(store, web).toTypedArray())
    }

    // --- Pantalla de inicio y foto de fondo --------------------------------------

    /**
     * Si al pulsar Inicio se abriría Doado. En Android 9 o antes, sin elegir aún,
     * Android contesta con su selector, que no es Doado, y se avisa. Si la consulta falla, se da por buena:
     * mejor sin aviso que con uno falso. Es una consulta al PackageManager, así
     * que va dentro de [loadApps], fuera del hilo principal.
     */
    private fun resolvesHomeToDoado(): Boolean {
        // Desde Android 10 se le pregunta al sistema quién tiene el papel de inicio.
        // Preguntar al PackageManager no vale: desde Android 11 Doado no ve los
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
     * Desde Android 10, el diálogo del sistema que pregunta si Doado pasa a ser la
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
        // Doado, donde el mismo aviso abre los de Android.
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

        /** El fundido entre pantallas: la nueva entra algo más despacio de lo que sale la vieja. */
        const val SCREEN_FADE_IN_MS = 180
        const val SCREEN_FADE_OUT_MS = 120
    }
}
