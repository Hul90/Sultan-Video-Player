package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vaultPinDataStore by preferencesDataStore(name = "vault_pin_prefs")

/**
 * Persists the Private Vault's PIN using DataStore so it survives app restarts.
 * Previously the PIN lived only in Compose `remember` state, which reset to the
 * hardcoded default ("1234") every time the vault screen was recomposed / the app
 * process restarted, and any "new PIN" the user set was silently lost.
 */
class VaultPinStore(private val context: Context) {

    companion object {
        private val PIN_KEY = stringPreferencesKey("vault_pin")
        const val DEFAULT_PIN = "1234"
    }

    val pinFlow: Flow<String> = context.vaultPinDataStore.data.map { prefs ->
        prefs[PIN_KEY] ?: DEFAULT_PIN
    }

    suspend fun setPin(newPin: String) {
        context.vaultPinDataStore.edit { prefs ->
            prefs[PIN_KEY] = newPin
        }
    }
}
