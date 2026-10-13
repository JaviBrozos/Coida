package com.doado.launcher

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import com.doado.data.AssistantSettings
import com.doado.data.ContactAction
import com.doado.data.ContactShortcut
import com.doado.data.ContactShortcutsStore
import com.doado.data.DefaultFavorites
import com.doado.data.FavoriteApp
import com.doado.data.FavoritesStore
import com.doado.data.InstalledApp
import com.doado.data.InstalledAppsRepository
import com.doado.data.InterfaceSettings
import com.doado.data.PinStore
import com.doado.data.SettingsStore
import com.doado.data.SuggestedApp
import com.doado.data.SuggestedApps
import com.doado.data.ThemeMode
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
import com.doado.settings.SuggestedAppRow
import java.util.Locale
import java.util.UUID

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

    private var screen by mutableStateOf(Screen.HOME)
    private var interfaceSettings by mutableStateOf(InterfaceSettings())
    private var homeApps by mutableStateOf(emptyList<FavoriteApp>())
    private var homeIds by mutableStateOf(emptyList<String>())
    private var contacts by mutableStateOf(emptyList<ContactShortcut>())
    private var adminUnlockedUntil by mutableLongStateOf(0L)
    private var installedSuggested by mutableStateOf(emptySet<String>())
    private var installedApps by mutableStateOf(emptyList<InstalledApp>())
    private var defaultPackages by mutableStateOf(emptySet<String>())

    private val assistantConfig = AssistantConfig.fromBuildConfig()
    private lateinit var assistant: AssistantController
    private var assistantSettings by mutableStateOf(AssistantSettings())
    private var assistantMicDenied by mutableStateOf(false)

    /** Casilla o contacto que el asistente ha señalado al guiar. Se quita al tocar algo. */
    private var highlightedId by mutableStateOf<String?>(null)

    /** A dónde vuelve el asistente al cerrarse: al inicio, o a sus ajustes si se estaba probando. */
    private var assistantReturn = Screen.HOME

    /** Apps que se mandaron instalar: al volver ya instaladas, se ponen en el inicio. */
    private val pendingInstalls = mutableSetOf<String>()

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
                        CurrentScreen()
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

        if (screen == Screen.HOME) {
            HomeScreen(
                apps = homeApps,
                contacts = contacts.filter { it.onHome }.map {
                    HomeContact(
                        shortcut = it,
                        isAvailable = it.action != ContactAction.WHATSAPP ||
                            WHATSAPP_PACKAGE in installedSuggested,
                    )
                },
                columns = interfaceSettings.columns,
                showClock = interfaceSettings.showClock,
                adminUnlockedUntil = adminUnlockedUntil,
                showAssistant = assistantSettings.enabled && assistantConfig.isConfigured,
                highlightedId = highlightedId,
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
            when (screen) {
                Screen.HOME -> Unit

                Screen.PIN -> PinGate(
                    hasPin = pinStore.hasPin(),
                    checkPin = pinStore::check,
                    savePin = pinStore::save,
                    onUnlocked = { screen = Screen.SETTINGS },
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
                )

                Screen.INTERFACE -> InterfaceScreen(
                    settings = interfaceSettings,
                    onChange = {
                        interfaceSettings = it
                        settingsStore.saveInterfaceSettings(it)
                    },
                    onBack = { screen = Screen.SETTINGS },
                )

                Screen.CONTACTS -> ContactsScreen(
                    contacts = contacts,
                    whatsappInstalled = WHATSAPP_PACKAGE in installedSuggested,
                    onAdd = ::launchContactPicker,
                    onUpdate = { updated ->
                        saveContacts(contacts.map { if (it.id == updated.id) updated else it })
                    },
                    onRemove = { removed -> saveContacts(contacts.filterNot { it.id == removed.id }) },
                    onBack = { screen = Screen.SETTINGS },
                )

                Screen.APPS -> AppsScreen(
                    basics = DefaultFavorites.map {
                        BasicAppRow(it.id, getString(it.labelRes), it.iconRes, it.id in homeIds)
                    },
                    suggested = SuggestedApps.map {
                        SuggestedAppRow(
                            app = it,
                            installed = it.packageName in installedSuggested,
                            onHome = FavoritesStore.packageId(it.packageName) in homeIds,
                        )
                    },
                    others = installedApps
                        .filter { app ->
                            app.packageName !in defaultPackages &&
                                SuggestedApps.none { it.packageName == app.packageName }
                        }
                        .map { OtherAppRow(it, FavoritesStore.packageId(it.packageName) in homeIds) },
                    adminActive = isAdminActive(),
                    onToggle = { id, onHome ->
                        repository.setOnHome(id, onHome)
                        reload()
                    },
                    onInstall = ::installSuggested,
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
        contacts = contactsStore.all()
        installedSuggested = SuggestedApps
            .map { it.packageName }
            .filter { repository.isInstalled(it) }
            .toSet()

        // Lo que se mandó instalar y ya está: directo al inicio.
        pendingInstalls.filter { it in installedSuggested }.forEach { packageName ->
            val id = FavoritesStore.packageId(packageName)
            if (id !in repository.homeIds()) repository.setOnHome(id, true)
            pendingInstalls.remove(packageName)
        }

        homeIds = repository.homeIds()
        homeApps = repository.homeApps()
        if (screen == Screen.APPS) loadInstalledApps()
    }

    private fun openAppsScreen() {
        loadInstalledApps()
        screen = Screen.APPS
    }

    private fun loadInstalledApps() {
        defaultPackages = repository.defaultFavoritePackages()
        installedApps = repository.installedApps()
    }

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
                whatsappInstalled = WHATSAPP_PACKAGE in installedSuggested,
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
            onHome = contacts.count { it.onHome } < ContactShortcutsStore.MAX_ON_HOME,
        )
        saveContacts(contacts + shortcut)

        // Se pide aquí, con el familiar delante, y no en el inicio con la persona mayor.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestCallPermission.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun installSuggested(app: SuggestedApp) {
        if (!isAdminActive()) {
            Toast.makeText(this, R.string.admin_required, Toast.LENGTH_LONG).show()
            screen = Screen.ADMIN
            return
        }
        pendingInstalls += app.packageName
        startFirst(*repository.storeIntents(app.packageName).toTypedArray(), label = app.label)
    }

    private fun openStore() {
        val store = packageManager.getLaunchIntentForPackage(PLAY_STORE_PACKAGE)
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps"))
        startFirst(*listOfNotNull(store, web).toTypedArray())
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
    }
}
