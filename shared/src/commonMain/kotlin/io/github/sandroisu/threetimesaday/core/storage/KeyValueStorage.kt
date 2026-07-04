package io.github.sandroisu.threetimesaday.core.storage

interface KeyValueStorage {

    fun getString(key: String): String?

    fun putString(key: String, value: String)

    fun remove(key: String)
}
