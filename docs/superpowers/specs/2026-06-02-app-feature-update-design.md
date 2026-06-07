# Luminine — App Feature & UI/UX Update Design

**Date:** 2026-06-02
**Branch:** `feature/app-feature-update` (off `develop`)
**Source spec:** `app_feature_update.md` (Korean product spec, 3 feature areas)

## Goal

Extend Luminine's commerce reach and accessibility for a broad age range, across three
feature areas:

1. **Navigation & layout** — add a `Shop` bottom tab; move 메뉴 (☰) and 마이페이지 (👤) to a
   pinned top-right app bar.
2. **In-app WebView** — load health-info content in-app with a nav control bar and a reading mode.
3. **Accessibility** — 4-level font scaling, dark mode (OS-synced), high-contrast mode, and a
   reading mode for web columns.

All backend, real commerce, and real content URLs remain behind clear next-phase boundaries,
consistent with the existing project convention (Kakao/Supabase/AI are already stubbed).

## Decisions (locked during brainstorming)

| Area | Decision |
|---|---|
| Bottom nav (5 tabs) | `홈 · 차트 · 건강정보 · 1:1케어 · Shop` — Shop appended **last** |
| 메뉴 / 마이페이지 | Move to top-right app bar as ☰ / 👤 icon buttons (TopAppBar pins on scroll) |
| Shop / WebView content | **Placeholder / sample** this phase, behind config seams |
| Theme behavior | Dark follows **OS by default**; manual 라이트/다크/시스템 override; **high-contrast is an independent toggle layered on top** |
| Font scale | 4 levels — 작게 ×0.85 / 보통 ×1.0 / 크게 ×1.25 / 아주 크게 ×1.5 |
| Settings persistence | DataStore-backed `SettingsRepository` (in-memory default + DataStore impl behind DI), per the onboarding pattern |
| WebView platform layer | `expect`/`actual` `@Composable PlatformWebView` — Android `WebView`, iOS `WKWebView` |
| Reading mode | Toggle on the native WebView that injects reader CSS via JS; honors the app font-scale. No HTML parser. |
| Settings → UI flow | CompositionLocal + reactive `Flow<LuminineSettings>` collected in `App()`, applied in `LuminineTheme(settings)` |

## Architecture

### 1. Navigation & layout

- `MainTab` enum (in `App.kt`) gains `Shop` as the **last** entry.
- `topLevelDestinations()` in `ui/UiContent.kt`: **drop 메뉴**, **add Shop** (icon `LuminineIcon.Shop`).
  The list must stay index-aligned with `MainTab.entries` — the `bottomBar` zips the two by index,
  so a mismatch is a latent bug. A unit test guards this alignment.
- `MainScaffold` gains an `overlay` state: `None | Menu | MyPage | ReadabilitySettings | WebView(url, title)`,
  rendered above the current tab content with a back affordance. No nav library — consistent with the
  existing hand-rolled `when`-based navigation.
- Top app bar `actions`: ☰ `전체메뉴` button (opens Menu overlay) + 👤 `마이페이지` button (opens MyPage overlay).
  The existing admin toggle is preserved.
- New screens extracted into `ui/screens/` rather than growing the 1200-line `App.kt`.

### 2. Accessibility — settings, theme, font scale

**Model** (`model/Settings.kt`):

```kotlin
enum class ThemeMode { System, Light, Dark }
enum class FontScale(val multiplier: Float) { Small(0.85f), Normal(1.0f), Large(1.25f), ExtraLarge(1.5f) }

@Serializable
data class LuminineSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val highContrast: Boolean = false,
    val fontScale: FontScale = FontScale.Normal,
)
```

**Persistence** (mirrors onboarding's `SessionRepository`/`SurveyRepository`):
- `data/settings/SettingsRepository.kt` — `fun observe(): Flow<LuminineSettings>`; `suspend fun update(transform: (LuminineSettings) -> LuminineSettings)`.
- `data/settings/InMemorySettingsRepository.kt` — `MutableStateFlow` default.
- `data/store/DataStoreSettingsRepository.kt` — persists via the existing DataStore; reuses `dataStorePath(...)`. `installDataStore()` stays idempotent.
- `di/LuminineDependencies.kt` — add `settingsRepository`.

**Theme flow** (CompositionLocal approach):
- `App()` collects `settingsRepository.observe()` into state, passes `settings` to `LuminineTheme(settings) { ... }`.
- `ui/theme/ColorSchemes.kt` — light (existing ivory/gold), dark, and high-contrast schemes, plus a **pure**
  `fun resolveColorScheme(mode: ThemeMode, highContrast: Boolean, systemDark: Boolean): ColorScheme`.
  `themeMode=System` reads `isSystemInDarkTheme()`; `highContrast=true` overrides to the HC scheme regardless of light/dark.
- Font scale: wrap content in `CompositionLocalProvider(LocalDensity provides Density(density, fontScale = settings.fontScale.multiplier))` so **all** `sp` text scales live — no per-typography rewrite.

**Settings screen** (`ui/screens/ReadabilitySettingsScreen.kt`, reached from MyPage → 화면/가독성 설정):
- Theme mode radio (라이트/다크/시스템), 고대비 모드 Switch (independent), 글자 크기 4-segment selector
  with a **live preview** card above. Each change calls `update{}`; the flow re-emits → recompose. No save button.

**Hardcoded color cleanup:** existing direct uses of `ReverseGold`/`ReverseEspresso` etc. in `App.kt`
(e.g. nav tint, icon tints) bypass the theme and won't react to dark/HC. Route the visible ones through
`MaterialTheme.colorScheme` as part of this work — targeted, not a blanket refactor.

### 3. In-app WebView + reading mode

**Platform seam** (`expect`/`actual`, same idiom as DataStore):

```kotlin
// commonMain ui/web/PlatformWebView.kt
@Composable
expect fun PlatformWebView(url: String, readingMode: Boolean, controller: WebViewController, modifier: Modifier = Modifier)

class WebViewController { /* canGoBack, canGoForward state + goBack()/goForward()/reload() */ }
expect fun openInExternalBrowser(url: String)
fun readerCss(fontScale: FontScale): String   // pure
```

- `androidMain/ui/web/PlatformWebView.android.kt` — `AndroidView { WebView }`; `WebViewClient` tracks history;
  reading mode injects `readerCss` via `evaluateJavascript`. `openInExternalBrowser` → `Intent.ACTION_VIEW`.
- `iosMain/ui/web/PlatformWebView.ios.kt` — `UIKitView { WKWebView }`; `WKNavigationDelegate` tracks history;
  reading mode injects via `evaluateJavaScript`. `openInExternalBrowser` → `UIApplication.openURL`.

**WebView screen** (`ui/screens/WebViewScreen.kt`, opened as the overlay):
- `PlatformWebView` + bottom control bar: ◀ 뒤로 · ▶ 앞으로 · ⟳ 새로고침 · ⤴ 외부 브라우저로 열기 · 읽기 모드 toggle.
- Back/forward enabled-state from `WebViewController`.

**Reading mode:** injects a reader stylesheet into the live page (wider `line-height`, larger base font,
hides `header/footer/nav/aside`, constrains content width). Base font honors the app `FontScale` multiplier,
so reading mode + font scale compose.

**Health-info integration:** `healthTopics()` cards in `HealthInfoScreen` become tappable → open `WebViewScreen`
with a **placeholder URL** per topic. `content/HealthContent.kt` (`fun urlFor(topic): String`) is the seam where
real URLs drop in later — a one-file change.

## File map

**New**
- `commonMain/.../model/Settings.kt`
- `commonMain/.../data/settings/SettingsRepository.kt`
- `commonMain/.../data/settings/InMemorySettingsRepository.kt`
- `commonMain/.../data/store/DataStoreSettingsRepository.kt`
- `commonMain/.../ui/theme/ColorSchemes.kt`
- `commonMain/.../ui/web/PlatformWebView.kt`
- `androidMain/.../ui/web/PlatformWebView.android.kt`
- `iosMain/.../ui/web/PlatformWebView.ios.kt`
- `commonMain/.../ui/screens/ShopScreen.kt`
- `commonMain/.../ui/screens/WebViewScreen.kt`
- `commonMain/.../ui/screens/MyPageScreen.kt`
- `commonMain/.../ui/screens/ReadabilitySettingsScreen.kt`
- `commonMain/.../ui/screens/MenuOverlay.kt` (extracted from existing MenuScreen)
- `commonMain/.../content/HealthContent.kt`

**Modified**
- `App.kt` — collect settings; `LuminineTheme(settings)`; top-bar ☰/👤; overlay state; Shop tab branch; route hardcoded colors through theme.
- `ui/theme/LuminineTheme.kt` — accept `settings`; apply `resolveColorScheme` + `LocalDensity` fontScale.
- `ui/UiContent.kt` — `topLevelDestinations()` drop 메뉴 / add Shop; `MainTab` enum gains Shop.
- `di/LuminineDependencies.kt` — add `settingsRepository`; install DataStore impl.

## Data flow

`SettingsRepository.observe()` → `App` state → `LuminineTheme(settings)` → `resolveColorScheme` +
`Density(fontScale=…)` → entire composable tree. Settings screen → `update{}` → repo re-emits → recompose.

## Testing (TDD, `commonTest`; runs on Android host)

- `resolveColorScheme(mode, highContrast, systemDark)` — full light/dark/HC matrix.
- `FontScale.multiplier` values; `LuminineSettings` defaults + JSON round-trip (`@Serializable`).
- `InMemorySettingsRepository` — observe/update emits new value.
- `WebViewController` — back/forward/reload state transitions.
- `readerCss(fontScale)` — contains widened line-height and scaled font.
- `HealthContent.urlFor(topic)` — mapping correctness.
- `topLevelDestinations()` ↔ `MainTab.entries` index alignment; Shop present, 메뉴 absent.

## Verification targets

(Per verified KMP build facts from the onboarding work.)
- `:composeApp:allTests` — real task is `:composeApp:testAndroidHostTest`; grep `BUILD SUCCESSFUL/FAILED` (tail exit codes can lie).
- `:androidApp:assembleDebug`.
- iOS link: `:composeApp:linkDebugFrameworkIosSimulatorArm64` (+ `IosArm64`).
- `kotlinx-coroutines-test` pinned **1.9.0**.

## Non-goals (next-phase boundaries)

- Real commerce backend / checkout / purchase history (Shop is a placeholder product grid).
- Real health-content URLs (placeholder via `HealthContent` seam).
- True Reader-style HTML content extraction (we inject reader CSS instead — works on real pages without a parser).
- A 커뮤니티 (community) screen — the spec's bottom-nav wording was illustrative only; not in scope.
