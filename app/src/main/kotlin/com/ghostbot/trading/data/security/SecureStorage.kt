package com.ghostbot.trading.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ghostbot.trading.domain.model.ApiKeys
import com.ghostbot.trading.domain.model.AuthToken
import com.ghostbot.trading.domain.model.LoginCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    
    private val masterKeyAlias by lazy {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "ghost_bot_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    // PIN Management
    fun savePin(pin: String) {
        val hashedPin = hashPin(pin)
        encryptedPrefs.edit().putString(KEY_PIN, hashedPin).apply()
    }
    
    fun verifyPin(pin: String): Boolean {
        val storedHash = encryptedPrefs.getString(KEY_PIN, null) ?: return false
        return hashPin(pin) == storedHash
    }
    
    fun isPinSet(): Boolean {
        return encryptedPrefs.getString(KEY_PIN, null) != null
    }
    
    // Biometric Settings
    fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }
    
    fun isBiometricEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    // Login Credentials
    fun saveLoginCredentials(credentials: LoginCredentials) {
        try {
            val jsonString = json.encodeToString(credentials)
            encryptedPrefs.edit().putString(KEY_LOGIN_CREDENTIALS, jsonString).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save login credentials")
        }
    }
    
    fun getLoginCredentials(): LoginCredentials? {
        return try {
            val jsonString = encryptedPrefs.getString(KEY_LOGIN_CREDENTIALS, null)
            jsonString?.let { json.decodeFromString<LoginCredentials>(it) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve login credentials")
            null
        }
    }
    
    fun clearLoginCredentials() {
        encryptedPrefs.edit().remove(KEY_LOGIN_CREDENTIALS).apply()
    }
    
    // Auth Tokens
    fun saveAuthToken(token: AuthToken) {
        try {
            val jsonString = json.encodeToString(token)
            encryptedPrefs.edit().putString(KEY_AUTH_TOKEN, jsonString).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save auth token")
        }
    }
    
    fun getAuthToken(): AuthToken? {
        return try {
            val jsonString = encryptedPrefs.getString(KEY_AUTH_TOKEN, null)
            jsonString?.let { json.decodeFromString<AuthToken>(it) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve auth token")
            null
        }
    }
    
    fun clearAuthToken() {
        encryptedPrefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }
    
    fun isTokenValid(): Boolean {
        val token = getAuthToken() ?: return false
        return System.currentTimeMillis() < token.expiresAt
    }
    
    // API Keys
    fun saveApiKeys(apiKeys: ApiKeys) {
        try {
            val jsonString = json.encodeToString(apiKeys)
            encryptedPrefs.edit().putString(KEY_API_KEYS, jsonString).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save API keys")
        }
    }
    
    fun getApiKeys(): ApiKeys? {
        return try {
            val jsonString = encryptedPrefs.getString(KEY_API_KEYS, null)
            jsonString?.let { json.decodeFromString<ApiKeys>(it) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve API keys")
            null
        }
    }
    
    fun clearApiKeys() {
        encryptedPrefs.edit().remove(KEY_API_KEYS).apply()
    }
    
    // Session Management
    fun saveLastLoginTime() {
        encryptedPrefs.edit().putLong(KEY_LAST_LOGIN, System.currentTimeMillis()).apply()
    }
    
    fun getLastLoginTime(): Long {
        return encryptedPrefs.getLong(KEY_LAST_LOGIN, 0)
    }
    
    fun saveFailedAttempts(attempts: Int) {
        encryptedPrefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
    }
    
    fun getFailedAttempts(): Int {
        return encryptedPrefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }
    
    fun clearFailedAttempts() {
        encryptedPrefs.edit().remove(KEY_FAILED_ATTEMPTS).apply()
    }
    
    fun saveLockoutEndTime(endTime: Long) {
        encryptedPrefs.edit().putLong(KEY_LOCKOUT_END, endTime).apply()
    }
    
    fun getLockoutEndTime(): Long {
        return encryptedPrefs.getLong(KEY_LOCKOUT_END, 0)
    }
    
    fun isLockedOut(): Boolean {
        val lockoutEnd = getLockoutEndTime()
        return System.currentTimeMillis() < lockoutEnd
    }
    
    // Clear all data
    fun clearAllData() {
        encryptedPrefs.edit().clear().apply()
    }
    
    private fun hashPin(pin: String): String {
        // Using a simple hash for demonstration - in production, use proper password hashing
        return pin.hashCode().toString()
    }
    
    companion object {
        private const val KEY_PIN = "pin"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LOGIN_CREDENTIALS = "login_credentials"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_API_KEYS = "api_keys"
        private const val KEY_LAST_LOGIN = "last_login"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_END = "lockout_end"
    }
}