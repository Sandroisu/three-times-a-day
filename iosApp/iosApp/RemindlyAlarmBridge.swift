import Foundation
import Shared
import SwiftUI
import UserNotifications

#if canImport(AlarmKit)
import AlarmKit
#endif

final class RemindlyAlarmBridge: NSObject, IosAlarmBridge {

    private let operationLock = NSLock()
    private var operationTask: Task<Void, Never>?

    func scheduleAlarm(
        notificationId: String,
        title: String,
        message: String,
        triggerAtEpochMilliseconds: Int64
    ) {
        let fireDate = Date(timeIntervalSince1970: Double(triggerAtEpochMilliseconds) / 1_000)
        if #available(iOS 26.0, *) {
            enqueue {
                await AlarmKitCoordinator.shared.schedule(
                    notificationId: notificationId,
                    title: title,
                    message: message,
                    fireDate: fireDate
                )
            }
        } else {
            scheduleFallbackNotification(
                notificationId: notificationId,
                title: title,
                message: message,
                fireDate: fireDate
            )
        }
    }

    func cancelAlarm(notificationId: String) {
        if #available(iOS 26.0, *) {
            enqueue { await AlarmKitCoordinator.shared.cancel(notificationId: notificationId) }
        }
    }

    func cancelAlarmsWithPrefix(notificationIdPrefix: String) {
        if #available(iOS 26.0, *) {
            enqueue { await AlarmKitCoordinator.shared.cancel(prefix: notificationIdPrefix) }
        }
    }

    private func enqueue(_ operation: @escaping @Sendable () async -> Void) {
        operationLock.lock()
        let previousTask = operationTask
        let nextTask = Task {
            _ = await previousTask?.result
            await operation()
        }
        operationTask = nextTask
        operationLock.unlock()
    }

    private func scheduleFallbackNotification(
        notificationId: String,
        title: String,
        message: String,
        fireDate: Date
    ) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = message
        content.sound = .default
        content.interruptionLevel = .timeSensitive
        let components = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: fireDate
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        let request = UNNotificationRequest(identifier: notificationId, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request)
    }
}

#if canImport(AlarmKit)
@available(iOS 26.0, *)
private struct RemindlyAlarmMetadata: AlarmMetadata {
    let notificationId: String
    let message: String
}

@available(iOS 26.0, *)
private actor AlarmKitCoordinator {

    static let shared = AlarmKitCoordinator()

    private let manager = AlarmManager.shared
    private let mappingKey = "remindly_alarmkit_notification_ids"
    private var authorizationTask: Task<AlarmManager.AuthorizationState, Error>?

    func schedule(
        notificationId: String,
        title: String,
        message: String,
        fireDate: Date
    ) async {
        guard await isAuthorized() else {
            scheduleFallbackNotification(
                notificationId: notificationId,
                title: title,
                message: message,
                fireDate: fireDate
            )
            return
        }
        let alarmID = alarmID(for: notificationId)
        let localizedTitle = LocalizedStringResource(String.LocalizationValue(title))
        let alert: AlarmPresentation.Alert
        if #available(iOS 26.1, *) {
            alert = AlarmPresentation.Alert(title: localizedTitle)
        } else {
            let stopButton = AlarmButton(
                text: LocalizedStringResource(String.LocalizationValue("Stop")),
                textColor: .white,
                systemImageName: "stop.fill"
            )
            alert = AlarmPresentation.Alert(title: localizedTitle, stopButton: stopButton)
        }
        let presentation = AlarmPresentation(alert: alert)
        let attributes = AlarmAttributes(
            presentation: presentation,
            metadata: RemindlyAlarmMetadata(notificationId: notificationId, message: message),
            tintColor: .blue
        )
        let configuration = AlarmManager.AlarmConfiguration<RemindlyAlarmMetadata>.alarm(
            schedule: .fixed(fireDate),
            attributes: attributes
        )
        do {
            try? manager.cancel(id: alarmID)
            _ = try await manager.schedule(id: alarmID, configuration: configuration)
        } catch {
            scheduleFallbackNotification(
                notificationId: notificationId,
                title: title,
                message: message,
                fireDate: fireDate
            )
        }
    }

    func cancel(notificationId: String) {
        guard let alarmID = storedAlarmID(for: notificationId) else { return }
        try? manager.cancel(id: alarmID)
        var mappings = storedMappings()
        mappings.removeValue(forKey: notificationId)
        store(mappings)
    }

    func cancel(prefix: String) {
        var mappings = storedMappings()
        let matchingNotificationIDs = mappings.keys.filter { notificationID in
            notificationID.hasPrefix(prefix)
        }
        for notificationID in matchingNotificationIDs {
            guard let alarmIDString = mappings.removeValue(forKey: notificationID),
                  let alarmID = UUID(uuidString: alarmIDString) else { continue }
            try? manager.cancel(id: alarmID)
        }
        store(mappings)
    }

    private func isAuthorized() async -> Bool {
        switch manager.authorizationState {
        case .authorized:
            return true
        case .denied:
            return false
        case .notDetermined:
            if let authorizationTask {
                return (try? await authorizationTask.value) == .authorized
            }
            let task = Task { try await manager.requestAuthorization() }
            authorizationTask = task
            let state = try? await task.value
            authorizationTask = nil
            return state == .authorized
        @unknown default:
            return false
        }
    }

    private func alarmID(for notificationId: String) -> UUID {
        if let storedID = storedAlarmID(for: notificationId) {
            return storedID
        }
        let newID = UUID()
        var mappings = storedMappings()
        mappings[notificationId] = newID.uuidString
        store(mappings)
        return newID
    }

    private func storedAlarmID(for notificationId: String) -> UUID? {
        guard let value = storedMappings()[notificationId] else { return nil }
        return UUID(uuidString: value)
    }

    private func storedMappings() -> [String: String] {
        UserDefaults.standard.dictionary(forKey: mappingKey) as? [String: String] ?? [:]
    }

    private func store(_ mappings: [String: String]) {
        UserDefaults.standard.set(mappings, forKey: mappingKey)
    }

    private func scheduleFallbackNotification(
        notificationId: String,
        title: String,
        message: String,
        fireDate: Date
    ) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = message
        content.sound = .default
        content.interruptionLevel = .timeSensitive
        let components = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: fireDate
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        let request = UNNotificationRequest(identifier: notificationId, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request)
    }
}
#endif
