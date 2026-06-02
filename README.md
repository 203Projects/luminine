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

- Onboarding: Kakao login (stubbed behind a `KakaoAuthClient` interface, `#FEE500` CTA) → a 7-section health survey (S0/S1/S6 required, S2–S5/S7 skippable with "나중에 입력") → the main app. Progress bar, sensitive-info notice before health questions, and a completion reward screen.
- Onboarding data drives the app: the Home greeting/avatar use the real name, an ordered S6 goal-chip row appears on Home, and default routines are pre-activated from the chosen goals + exercise level. The survey is summarized in 메뉴, with skipped sections surfaced for later entry.
- Session + survey persist on-device via Kotlin Multiplatform DataStore (behind `SessionRepository`/`SurveyRepository` interfaces; in-memory fallback keeps the build green). Auth/persistence are swappable seams for real Kakao/Supabase later.
- Home routine tracker with category filters, custom routine add/delete, completion score, condition sliders, emoji state, and save action.
- Charts tab with monthly completion buckets, 7-day condition bars, routine ranking, and AI insight card.
- Health information tab with anti-aging category cards, article list, and YouTube placeholder. Topic cards open an in-app WebView.
- 1:1 care tab with Kakao connection CTA, care checklist, inbody card, parser preview, and AI advice card.
- Shop tab (bottom nav): placeholder beauty/healthcare commerce grid.
- Top app bar with always-accessible 전체 메뉴 (☰) and 마이페이지 (👤) icons; the former 메뉴 tab content now opens as a full-screen overlay from ☰.
- In-app WebView for health-info content: bottom control bar (back / forward / refresh / open-in-external-browser) plus a 읽기 모드 (reading mode) toggle that injects reader CSS (wider line spacing, hidden chrome) honoring the app font scale.
- Accessibility — 화면·가독성 설정 (reached from 메뉴 or 마이페이지): 4-level font scaling (작게/보통/크게/아주 크게) applied app-wide, dark mode (follows the OS by default with a 라이트/다크/시스템 override), and an independent 고대비 (high-contrast) toggle. Settings persist via DataStore behind a `SettingsRepository` seam.
- Admin mode with status, member records, analysis, attention-member highlighting, and Kakao receive logs.
- Shared Kakao message parser for inbody, diet, and supplement messages.

Backend, Kakao OAuth/webhooks, Supabase, push notifications, and production AI API calls are intentionally left behind clear integration boundaries for the next phase. Likewise, Shop products are sample data and health-info content uses placeholder URLs — real content URLs swap in by editing `content/HealthContent.urlFor()` only.

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
