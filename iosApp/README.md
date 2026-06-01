# iOS app shell

The shared Compose UI is exported from `:composeApp` as the `Luminine` framework.

The included `iosApp.xcodeproj` links the static KMP framework and runs this pre-build script:

```sh
cd "$SRCROOT/.."
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

The Swift entry point calls `MainViewController()` from the generated Kotlin framework.

To run on a simulator:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath iosApp/build \
  CODE_SIGNING_ALLOWED=NO \
  build
```
