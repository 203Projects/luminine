# iOS app shell

The shared Compose UI is exported from `:composeApp` as the `ReverseHealthTracker` framework.

Create or open an Xcode iOS app target named `iosApp`, add the Swift files in `iosApp/iosApp`, and add a pre-build script that runs:

```sh
cd "$SRCROOT/.."
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

The Swift entry point calls `MainViewController()` from the generated Kotlin framework.
