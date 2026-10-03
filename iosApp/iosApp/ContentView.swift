import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    private let alarmBridge = RemindlyAlarmBridge()

    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(alarmBridge: alarmBridge)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
