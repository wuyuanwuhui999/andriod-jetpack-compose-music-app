package com.player.music.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.player.music.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * 本地数据存储管理类：token、用户信息、登录状态等
 */
class DataStoreManager(private val context: Context) {
    private val gson = Gson()

    /** 保存 token */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(PreferenceKeys.TOKEN)] = token
        }
    }

    /** 获取 token */
    fun getToken(): Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[stringPreferencesKey(PreferenceKeys.TOKEN)]
        }

    /** 保存用户信息 */
    suspend fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(PreferenceKeys.USER_DATA)] = userJson
        }
    }

    /** 获取用户信息 */
    fun getUser(): Flow<User?> = context.dataStore.data
        .map { preferences ->
            val userJson = preferences[stringPreferencesKey(PreferenceKeys.USER_DATA)]
            userJson?.let { gson.fromJson(it, User::class.java) }
        }

    /** 设置登录状态 */
    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(PreferenceKeys.IS_LOGGED_IN)] = isLoggedIn
        }
    }

    /** 获取登录状态 */
    fun isLoggedIn(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[booleanPreferencesKey(PreferenceKeys.IS_LOGGED_IN)] ?: false
        }

    /** 保存字符串（通用方法） */
    suspend fun saveString(key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    /** 读取字符串（通用方法） */
    fun getString(key: String): Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[stringPreferencesKey(key)]
        }

    /** 清空所有数据（退出登录） */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
