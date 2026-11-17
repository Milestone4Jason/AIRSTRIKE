import SwiftUI
import GameController

@main
struct AIRSTRIKEApp: App {
    var body: some Scene {
        WindowGroup {
            GameScreen()
                .preferredColorScheme(.dark)
        }
    }
}

struct GameScreen: View {
    @State private var scene = FlightScene()

    var body: some View {
        ZStack(alignment: .topLeading) {
            SceneView(scene: scene, options: [.allowsCameraControl])
                .ignoresSafeArea()
            VStack(alignment: .leading, spacing: 6) {
                Text("AIRSTRIKE")
                    .font(.system(size: 25, weight: .black, design: .rounded))
                Text("SPEED  \(Int(scene.speed * 240)) km/h")
                    .font(.system(.caption, design: .monospaced).weight(.bold))
                Text(scene.controllerConnected ? "DUALSENSE CONNECTED" : "TOUCH / CONTROLLER")
                    .font(.system(size: 10, weight: .bold, design: .monospaced))
                    .foregroundStyle(scene.controllerConnected ? .green : .white.opacity(0.7))
            }
            .padding(14)
            .background(.black.opacity(0.45), in: RoundedRectangle(cornerRadius: 14))
            .padding()
            Spacer()
            HStack {
                Button("SLOW") { scene.speed = max(0.15, scene.speed - 0.1) }
                Spacer()
                Button("BOOST") { scene.speed = min(1.0, scene.speed + 0.2) }
            }
            .buttonStyle(.borderedProminent)
            .padding(.horizontal, 24)
            .padding(.bottom, 28)
            .frame(maxHeight: .infinity, alignment: .bottom)
        }
        .onAppear { scene.connectController() }
    }
}
