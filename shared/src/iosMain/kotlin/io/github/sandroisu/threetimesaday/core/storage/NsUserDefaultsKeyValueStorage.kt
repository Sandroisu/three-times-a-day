package io.github.sandroisu.threetimesaday.core.storage

import platform.Foundation.NSUserDefaults

class NsUserDefaultsKeyValueStorage(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : KeyValueStorage {

    override fun getString(key: String): String? = userDefaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        userDefaults.setObject(value, key)
    }

    override fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
    }
}
