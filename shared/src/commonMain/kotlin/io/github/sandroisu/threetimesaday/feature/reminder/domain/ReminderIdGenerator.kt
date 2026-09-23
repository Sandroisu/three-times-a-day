package io.github.sandroisu.threetimesaday.feature.reminder.domain

interface ReminderIdGenerator {

    suspend fun nextId(): String
}
