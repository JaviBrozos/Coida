package com.doado.assistant

import android.content.res.Resources
import com.doado.R
import com.doado.data.ContactAction
import com.doado.data.ContactShortcut
import com.doado.data.ContactsLayout
import com.doado.data.FavoriteApp
import com.doado.data.FavoritesStore
import com.doado.data.PHONE_FAVORITE_ID
import com.doado.data.WHATSAPP_PACKAGE

/**
 * Pasos para guiar hacia algo de Doado (modo «Explica cómo», D-27).
 *
 * Los redacta el código y no el modelo: sabemos exactamente dónde está cada botón
 * en este inicio, y un paso equivocado a una persona mayor la deja peor que antes.
 * Cada plan dice también qué casilla o contacto marcar al volver al inicio.
 */
object GuideSteps {

    class Plan(val steps: List<String>, val highlightId: String?)

    fun forAction(res: Resources, action: AssistantAction, catalog: ActionCatalog): Plan =
        when (action) {
            is AssistantAction.CallContact -> call(res, action.contact, catalog)
            is AssistantAction.WhatsAppContact -> whatsApp(res, action.contact, catalog)
            is AssistantAction.OpenApp -> Plan(
                steps = listOf(tileWhere(res, action.app, catalog), res.getString(R.string.guide_tile_press)),
                highlightId = action.app.id,
            )
        }

    private fun call(res: Resources, contact: ContactShortcut, catalog: ActionCatalog): Plan {
        catalog.homeButton(contact, ContactAction.CALL)?.let { button ->
            return Plan(
                steps = listOf(
                    contactWhere(res, button, catalog),
                    res.getString(R.string.guide_contact_press_call),
                ),
                highlightId = button.id,
            )
        }
        val phone = catalog.homeApps.firstOrNull { it.id == PHONE_FAVORITE_ID && it.isAvailable }
            ?: return Plan(listOf(res.getString(R.string.guide_ask_family_call, contact.name)), null)
        return Plan(
            steps = listOf(
                tileWhere(res, phone, catalog),
                res.getString(R.string.guide_call_search, contact.name),
            ),
            highlightId = phone.id,
        )
    }

    private fun whatsApp(res: Resources, contact: ContactShortcut, catalog: ActionCatalog): Plan {
        catalog.homeButton(contact, ContactAction.WHATSAPP)?.let { button ->
            return Plan(
                steps = listOf(
                    contactWhere(res, button, catalog),
                    res.getString(R.string.guide_contact_press_whatsapp),
                ),
                highlightId = button.id,
            )
        }
        val whatsApp = catalog.homeApps.firstOrNull {
            it.id == FavoritesStore.packageId(WHATSAPP_PACKAGE) && it.isAvailable
        } ?: return Plan(listOf(res.getString(R.string.guide_ask_family_whatsapp, contact.name)), null)
        return Plan(
            steps = listOf(
                tileWhere(res, whatsApp, catalog),
                res.getString(R.string.guide_whatsapp_search, contact.name),
            ),
            highlightId = whatsApp.id,
        )
    }

    /**
     * Los accesos directos van debajo de las apps y tras el rótulo «Tus contactos»,
     * que empieza fila (D-38): se cuentan desde ahí. Si van en su propia pantalla
     * (D-46), el inicio pasa solo a ella al señalarlo, y se cuenta desde arriba de
     * esa pantalla; se dice también cómo llegar, para la próxima vez.
     */
    private fun contactWhere(res: Resources, contact: ContactShortcut, catalog: ActionCatalog): String {
        val index = catalog.homeContacts.indexOfFirst { it.id == contact.id }.coerceAtLeast(0)
        val columns = catalog.columns
        val paged = catalog.contactsLayout == ContactsLayout.PAGE
        return when {
            paged && columns == 1 ->
                res.getString(R.string.guide_contact_where_page_one, contact.name, index + 1)
            paged -> res.getString(
                R.string.guide_contact_where_page_grid,
                contact.name,
                index / columns + 1,
                side(res, index, columns),
            )
            columns == 1 -> res.getString(R.string.guide_contact_where_one, contact.name, index + 1)
            else -> res.getString(
                R.string.guide_contact_where_grid,
                contact.name,
                index / columns + 1,
                side(res, index, columns),
            )
        }
    }

    /** Dónde está una casilla, contado como lo contaría alguien que la señala con el dedo. */
    private fun tileWhere(res: Resources, app: FavoriteApp, catalog: ActionCatalog): String {
        val index = catalog.homeApps.indexOfFirst { it.id == app.id }.coerceAtLeast(0)
        val columns = catalog.columns
        return if (columns == 1) {
            res.getString(R.string.guide_tile_where_one, app.label, index + 1)
        } else {
            res.getString(R.string.guide_tile_where_grid, app.label, index / columns + 1, side(res, index, columns))
        }
    }

    /** Con dos por fila, izquierda o derecha; con tres, también el centro (D-45). */
    private fun side(res: Resources, index: Int, columns: Int): String {
        val column = index % columns
        return res.getString(
            when (column) {
                0 -> R.string.guide_side_left
                columns - 1 -> R.string.guide_side_right
                else -> R.string.guide_side_center
            },
        )
    }
}
