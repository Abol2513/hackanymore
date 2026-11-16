package com.example

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.hexforge.core.common.AppLanguage
import io.hexforge.core.common.ReportLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.forgeDataStore by preferencesDataStore(name = "hexforge_settings")

data class ForgePreferences(
    val legalAccepted: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val reportLanguage: ReportLanguage = ReportLanguage.ENGLISH,
    val usePersianDigits: Boolean = false,
)

class ForgePreferencesRepository(private val context: Context) {
    private object Keys {
        val LEGAL_ACCEPTED = booleanPreferencesKey("legal_accepted")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val REPORT_LANGUAGE = stringPreferencesKey("report_language")
        val PERSIAN_DIGITS = booleanPreferencesKey("persian_digits")
    }

    val preferences: Flow<ForgePreferences> = context.forgeDataStore.data.map { prefs ->
        val defaultLang = if (context.resources.configuration.locales[0].language == "fa") {
            AppLanguage.PERSIAN
        } else {
            AppLanguage.ENGLISH
        }
        val lang = prefs[Keys.APP_LANGUAGE]?.let { stored ->
            AppLanguage.entries.firstOrNull { it.name == stored }
        } ?: defaultLang

        val repLang = prefs[Keys.REPORT_LANGUAGE]?.let { stored ->
            ReportLanguage.entries.firstOrNull { it.name == stored }
        } ?: if (lang == AppLanguage.PERSIAN) ReportLanguage.PERSIAN else ReportLanguage.ENGLISH

        ForgePreferences(
            legalAccepted = prefs[Keys.LEGAL_ACCEPTED] ?: false,
            appLanguage = lang,
            reportLanguage = repLang,
            usePersianDigits = prefs[Keys.PERSIAN_DIGITS] ?: (lang == AppLanguage.PERSIAN),
        )
    }

    suspend fun setLegalAccepted(accepted: Boolean) {
        context.forgeDataStore.edit { it[Keys.LEGAL_ACCEPTED] = accepted }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.forgeDataStore.edit {
            it[Keys.APP_LANGUAGE] = language.name
            if (language == AppLanguage.PERSIAN) {
                it[Keys.REPORT_LANGUAGE] = ReportLanguage.PERSIAN.name
            } else if (language == AppLanguage.ENGLISH) {
                it[Keys.REPORT_LANGUAGE] = ReportLanguage.ENGLISH.name
            }
        }
    }

    suspend fun setReportLanguage(language: ReportLanguage) {
        context.forgeDataStore.edit { it[Keys.REPORT_LANGUAGE] = language.name }
    }

    suspend fun setUsePersianDigits(enabled: Boolean) {
        context.forgeDataStore.edit { it[Keys.PERSIAN_DIGITS] = enabled }
    }
}
