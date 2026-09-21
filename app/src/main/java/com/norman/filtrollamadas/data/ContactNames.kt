package com.norman.filtrollamadas.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.norman.filtrollamadas.domain.PhoneNumbers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Resuelve nombres de la agenda para mostrarlos en las listas, con caché en memoria.
 * Sirve también para llamadas registradas antes de que se guardara el nombre.
 */
class ContactNames(private val context: Context) {

    private val cache = ConcurrentHashMap<String, String>()
    private val misses: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    suspend fun lookup(rawNumber: String?): String? {
        val key = PhoneNumbers.key(rawNumber)
        if (key.isEmpty()) return null
        cache[key]?.let { return it }
        if (key in misses) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val name = withContext(Dispatchers.IO) { query(rawNumber.orEmpty()) }
        if (name != null) cache[key] = name else misses += key
        return name
    }

    /** La agenda pudo cambiar: se llama al volver a la app. */
    fun invalidate() {
        cache.clear()
        misses.clear()
    }

    private fun query(number: String): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.takeIf { it.isNotBlank() } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
