package com.mindgate.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mindgate.app.data.model.AppConfig
import com.mindgate.app.data.model.AppSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mindgate_prefs")

class MindGateDataStore(private val context: Context) {
    private val gson = Gson()

    companion object {
        val APP_CONFIGS_KEY = stringPreferencesKey("app_configs")
        val APP_SESSIONS_KEY = stringPreferencesKey("app_sessions")
        val UNLOCK_GATE_ENABLED = booleanPreferencesKey("unlock_gate_enabled")
        val UNLOCK_GATE_MESSAGE = stringPreferencesKey("unlock_gate_message")
        val UNLOCK_GATE_DELAY = intPreferencesKey("unlock_gate_delay")
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
    }

    val appConfigs: Flow<List<AppConfig>> = context.dataStore.data.map { prefs ->
        val json = prefs[APP_CONFIGS_KEY] ?: "[]"
        val type = object : TypeToken<List<AppConfig>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    }

    val appSessions: Flow<Map<String, AppSession>> = context.dataStore.data.map { prefs ->
        val json = prefs[APP_SESSIONS_KEY] ?: "{}"
        val type = object : TypeToken<Map<String, AppSession>>() {}.type
        gson.fromJson(json, type) ?: emptyMap()
    }

    val unlockGateEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[UNLOCK_GATE_ENABLED] ?: false
    }

    val unlockGateMessage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[UNLOCK_GATE_MESSAGE] ?: "শুভেচ্ছা! আজকের দিনটা সুন্দর করো।"
    }

    val unlockGateDelay: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[UNLOCK_GATE_DELAY] ?: 3
    }

    val serviceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SERVICE_ENABLED] ?: false
    }

    suspend fun saveAppConfig(config: AppConfig) {
        context.dataStore.edit { prefs ->
            val json = prefs[APP_CONFIGS_KEY] ?: "[]"
            val type = object : TypeToken<List<AppConfig>>() {}.type
            val list: MutableList<AppConfig> = gson.fromJson(json, type) ?: mutableListOf()
            list.removeAll { it.packageName == config.packageName }
            list.add(config)
            prefs[APP_CONFIGS_KEY] = gson.toJson(list)
        }
    }

    suspend fun removeAppConfig(packageName: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[APP_CONFIGS_KEY] ?: "[]"
            val type = object : TypeToken<List<AppConfig>>() {}.type
            val list: MutableList<AppConfig> = gson.fromJson(json, type) ?: mutableListOf()
            list.removeAll { it.packageName == packageName }
            prefs[APP_CONFIGS_KEY] = gson.toJson(list)
        }
    }

    suspend fun saveSession(session: AppSession) {
        context.dataStore.edit { prefs ->
            val json = prefs[APP_SESSIONS_KEY] ?: "{}"
            val type = object : TypeToken<Map<String, AppSession>>() {}.type
            val map: MutableMap<String, AppSession> = gson.fromJson(json, type) ?: mutableMapOf()
            map[session.packageName] = session
            prefs[APP_SESSIONS_KEY] = gson.toJson(map)
        }
    }

    suspend fun clearSession(packageName: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[APP_SESSIONS_KEY] ?: "{}"
            val type = object : TypeToken<Map<String, AppSession>>() {}.type
            val map: MutableMap<String, AppSession> = gson.fromJson(json, type) ?: mutableMapOf()
            map.remove(packageName)
            prefs[APP_SESSIONS_KEY] = gson.toJson(map)
        }
    }

    suspend fun setUnlockGate(enabled: Boolean, message: String, delay: Int) {
        context.dataStore.edit { prefs ->
            prefs[UNLOCK_GATE_ENABLED] = enabled
            prefs[UNLOCK_GATE_MESSAGE] = message
            prefs[UNLOCK_GATE_DELAY] = delay
        }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SERVICE_ENABLED] = enabled
        }
    }
}
