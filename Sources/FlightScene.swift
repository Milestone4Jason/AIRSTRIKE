import SceneKit
import GameController
import UIKit

final class FlightScene: SCNScene {
    var speed: CGFloat = 0.45
    var controllerConnected = false
    private let plane = SCNNode()
    private var cameraNode = SCNNode()
    private var tick: TimeInterval = 0

    override init() {
        super.init()
        background.contents = UIColor(red: 0.22, green: 0.55, blue: 0.82, alpha: 1)
        buildWorld()
        observeControllers()
        isPaused = false
        let loop = SCNAction.repeatForever(.sequence([
            .run { [weak self] _ in self?.updateFlight() },
            .wait(duration: 1.0 / 60.0)
        ]))
        rootNode.runAction(loop)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    private func buildWorld() {
        let camera = SCNCamera()
        cameraNode.camera = camera
        cameraNode.position = SCNVector3(0, 3.5, 11)
        cameraNode.eulerAngles.x = -0.18
        rootNode.addChildNode(cameraNode)

        let body = SCNCapsule(capRadius: 0.32, height: 3.0)
        body.firstMaterial?.diffuse.contents = UIColor(red: 0.82, green: 0.86, blue: 0.9, alpha: 1)
        plane.geometry = body
        plane.eulerAngles.z = .pi / 2
        rootNode.addChildNode(plane)

        let wing = SCNBox(width: 0.18, height: 0.08, length: 7.0, chamferRadius: 0.08)
        wing.firstMaterial?.diffuse.contents = UIColor.systemBlue
        let wingNode = SCNNode(geometry: wing)
        plane.addChildNode(wingNode)

        let tail = SCNBox(width: 0.12, height: 0.06, length: 2.0, chamferRadius: 0.04)
        tail.firstMaterial?.diffuse.contents = UIColor.systemBlue
        let tailNode = SCNNode(geometry: tail)
        tailNode.position = SCNVector3(-1.25, 0.25, 0)
        plane.addChildNode(tailNode)

        let sun = SCNLight()
        sun.type = .omni
        sun.intensity = 1100
        let sunNode = SCNNode()
        sunNode.light = sun
        sunNode.position = SCNVector3(0, 8, 5)
        rootNode.addChildNode(sunNode)

        let floor = SCNFloor()
        floor.reflectivity = 0
        floor.firstMaterial?.diffuse.contents = UIColor(red: 0.16, green: 0.42, blue: 0.34, alpha: 1)
        let floorNode = SCNNode(geometry: floor)
        floorNode.position = SCNVector3(0, -8, 0)
        rootNode.addChildNode(floorNode)
    }

    func connectController() {
        GCController.startWirelessControllerDiscovery {}
        observeControllers()
    }

    private func observeControllers() {
        NotificationCenter.default.addObserver(forName: .GCControllerDidConnect, object: nil, queue: .main) { [weak self] note in
            guard let controller = note.object as? GCController else { return }
            self?.controllerConnected = true
            self?.configure(controller)
        }
        NotificationCenter.default.addObserver(forName: .GCControllerDidDisconnect, object: nil, queue: .main) { [weak self] _ in
            self?.controllerConnected = false
        }
        GCController.controllers().forEach { configure($0) }
    }

    private func configure(_ controller: GCController) {
        guard let pad = controller.extendedGamepad else { return }
        pad.leftThumbstick.valueChangedHandler = { [weak self] _, x, y in
            DispatchQueue.main.async {
                self?.plane.eulerAngles.z = -x * 0.55
                self?.plane.eulerAngles.x = y * 0.35
            }
        }
        pad.rightTrigger.valueChangedHandler = { [weak self] _, value, _ in
            DispatchQueue.main.async { self?.speed = min(1, max(0.15, 0.35 + CGFloat(value) * 0.65)) }
        }
        pad.leftTrigger.valueChangedHandler = { [weak self] _, value, _ in
            DispatchQueue.main.async { self?.speed = max(0.15, 0.55 - CGFloat(value) * 0.4) }
        }
        pad.buttonA.pressedChangedHandler = { [weak self] _, _, pressed in
            if pressed { DispatchQueue.main.async { self?.speed = min(1, (self?.speed ?? 0.4) + 0.2) } }
        }
    }

    private func updateFlight() {
        tick += 1.0 / 60.0
        plane.position.y = Float(sin(tick * 1.2) * 0.18)
        plane.position.z -= Float(speed * 0.015)
        cameraNode.position.x += (plane.position.x - cameraNode.position.x) * 0.02
        cameraNode.position.y += (3.5 + plane.position.y - cameraNode.position.y) * 0.02
    }
}
