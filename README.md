# AIRSTRIKE

A simple SceneKit flight game for iPhone with PlayStation DualSense and other supported game controllers.

## Controls
- Left stick: steer
- R2: accelerate
- L2: slow down
- X / Cross: boost
- Triangle: change camera view
- Options / Menu: pause

## Build
This repository uses XcodeGen. Codemagic installs XcodeGen, generates `AIRSTRIKE.xcodeproj`, and builds an iOS IPA artifact.

**Signing note:** building an IPA and installing it on an iPhone are separate steps. Device installation requires a valid Apple signing identity and provisioning profile. A free Apple ID does not provide Codemagic's normal automatic distribution signing.

## Requirements
- Xcode 16 or newer
- XcodeGen
- iOS 16 or newer