package io.github.sandroisu.threetimesaday.core.notification

interface IosAlarmBridge {

    fun scheduleAlarm(
        notificationId: String,
        title: String,
        message: String,
        triggerAtEpochMilliseconds: Long,
    )

    fun cancelAlarm(notificationId: String)

    fun cancelAlarmsWithPrefix(notificationIdPrefix: String)
}
