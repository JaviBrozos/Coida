package com.coida.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coida.R
import com.coida.core.ui.CoidaCard
import com.coida.core.ui.TabularFigures
import com.coida.core.ui.IconBadge
import com.coida.core.ui.NoteCard
import com.coida.core.ui.OutlineButton
import com.coida.core.ui.PrimaryButton
import com.coida.core.ui.ScreenHeader
import com.coida.core.ui.SettingsColumn
import kotlinx.coroutines.delay

/** Lo que dura el desbloqueo. Largo para instalar una app, corto para no olvidarlo abierto. */
const val ADMIN_UNLOCK_MINUTES = 15

/**
 * Ajustes → Modo administrador.
 *
 * Coida no puede impedir por sí mismo que alguien salga a Android (eso exigiría
 * que la app fuera "propietaria del dispositivo"). Lo que hace este modo es
 * guardar bajo llave, y durante un tiempo limitado, las salidas que Coida sí
 * ofrece: la tienda para instalar apps, los ajustes de Android y el cambio de
 * pantalla de inicio. Al acabar el tiempo se cierra solo.
 */
@Composable
fun AdminScreen(
    unlockedUntil: Long,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onChooseHome: () -> Unit,
    onBack: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(unlockedUntil) {
        now = System.currentTimeMillis()
        while (now < unlockedUntil) {
            delay(1_000L)
            now = System.currentTimeMillis()
        }
    }
    val remainingMs = (unlockedUntil - now).coerceAtLeast(0L)
    val active = remainingMs > 0L

    SettingsColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings_admin),
            backLabel = stringResource(R.string.cd_back_settings),
            onBack = onBack,
        )

        CoidaCard(onClick = null, highlighted = active, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IconBadge(
                        icon = R.drawable.ic_shield,
                        size = 56.dp,
                        tint = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    ) {
                        Text(
                            text = stringResource(if (active) R.string.admin_unlocked else R.string.admin_locked),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                if (active) R.string.admin_unlocked_detail else R.string.admin_locked_detail,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (active) {
                    val totalSeconds = (remainingMs + 999) / 1000
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60),
                            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = TabularFigures),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.admin_countdown_suffix),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    OutlineButton(text = stringResource(R.string.admin_lock_now), onClick = onLock)
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.admin_unlock, ADMIN_UNLOCK_MINUTES),
                        onClick = onUnlock,
                    )
                }
            }
        }

        // Apagados mientras no hay modo administrador. Cada uno dice por qué lo
        // está: el gris, por sí solo, no es información para quien no lo ve.
        OutlineButton(
            text = stringResource(R.string.admin_open_store),
            icon = R.drawable.ic_store,
            onClick = onOpenStore,
            enabled = active,
            disabledReason = stringResource(
                R.string.cd_disabled_admin,
                stringResource(R.string.admin_open_store),
            ),
        )
        OutlineButton(
            text = stringResource(R.string.admin_open_android_settings),
            icon = R.drawable.ic_gear,
            onClick = onOpenAndroidSettings,
            enabled = active,
            disabledReason = stringResource(
                R.string.cd_disabled_admin,
                stringResource(R.string.admin_open_android_settings),
            ),
        )
        OutlineButton(
            text = stringResource(R.string.admin_choose_home),
            icon = R.drawable.ic_home,
            onClick = onChooseHome,
            enabled = active,
            disabledReason = stringResource(
                R.string.cd_disabled_admin,
                stringResource(R.string.admin_choose_home),
            ),
        )

        NoteCard(text = stringResource(R.string.admin_note, ADMIN_UNLOCK_MINUTES))
    }
}
