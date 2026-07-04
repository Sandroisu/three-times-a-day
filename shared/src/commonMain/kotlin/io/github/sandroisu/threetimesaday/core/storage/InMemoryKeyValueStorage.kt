package io.github.sandroisu.threetimesaday.core.storage

class InMemoryKeyValueStorage : KeyValueStorage {

    private val entries: MutableMap<String, String> = mutableMapOf()

    override fun getString(key: String): String? = entries[key]

    override fun putString(key: String, value: String) {
        entries[key] = value
    }

    override fun remove(key: String) {
        entries.remove(key)
    }
}
