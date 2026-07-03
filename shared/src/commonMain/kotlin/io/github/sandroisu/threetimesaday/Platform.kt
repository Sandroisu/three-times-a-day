package io.github.sandroisu.threetimesaday

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform