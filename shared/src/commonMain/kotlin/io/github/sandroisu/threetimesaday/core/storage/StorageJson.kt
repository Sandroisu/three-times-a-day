package io.github.sandroisu.threetimesaday.core.storage

import kotlinx.serialization.json.Json

// The only Json for data in KeyValueStorage. Android receivers and the iOS tap
// handler have no DI and use it directly, so all writers and readers agree.
// Changing a flag here changes the format of data users already saved.
internal val storageJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
