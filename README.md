# Luminine

Kotlin Multiplatform implementation of the Luminine anti-aging routine tracker for Android and iOS.

## Stack

- Kotlin Multiplatform `2.3.21`
- Compose Multiplatform `1.11.0`
- Compose Material 3 `1.9.0`
- Android Gradle Plugin `9.2.1`
- Gradle wrapper `9.5.1`

The project uses the AGP 9 split structure:

- `composeApp`: shared Kotlin Multiplatform domain, parser, sample data, and Compose UI.
- `androidApp`: Android application shell.
- `iosApp`: SwiftUI shell files that host the generated KMP framework.

## Implemented MVP Surface

- Home routine tracker with category filters, custom routine add/delete, completion score, condition sliders, emoji state, and save action.
- Charts tab with monthly completion buckets, 7-day condition bars, routine ranking, and AI insight card.
- Health information tab with anti-aging category cards, article list, and YouTube placeholder.
- 1:1 care tab with Kakao connection CTA, care checklist, inbody card, parser preview, and AI advice card.
- Menu tab with user points and Luminine service links.
- Admin mode with status, member records, analysis, attention-member highlighting, and Kakao receive logs.
- Shared Kakao message parser for inbody, diet, and supplement messages.

Backend, Kakao OAuth/webhooks, Supabase, push notifications, and production AI API calls are intentionally left behind clear integration boundaries for the next phase.

## Run

```sh
./gradlew :androidApp:assembleDebug
```

The debug APK is generated at:

```text
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

For iOS simulator:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath iosApp/build \
  CODE_SIGNING_ALLOWED=NO \
  build
```

The Swift files in `iosApp/iosApp` call `MainViewController()` from the generated `Luminine` framework. See `iosApp/README.md` for Xcode details.

## Test

```sh
./gradlew :composeApp:allTests
```

## Local Notes

`local.properties` is intentionally ignored. On this machine it points to:

```text
/Users/hanshin/Library/Android/sdk
```
