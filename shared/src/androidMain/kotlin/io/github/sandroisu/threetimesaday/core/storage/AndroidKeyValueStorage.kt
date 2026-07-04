package io.github.sandroisu.threetimesaday.core.storage

import android.content.Context

class AndroidKeyValueStorage(
    context: Context
) : KeyValueStorage {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "three_times_a_day_storage"
    }
}
