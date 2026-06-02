# App Feature & UI/UX Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Shop bottom tab + top-bar 메뉴/마이페이지, an in-app WebView with reading mode, and accessibility (4-level font scaling, dark + high-contrast theming) to the Luminine KMP app.

**Architecture:** Settings persist through a DataStore-backed `SettingsRepository` (in-memory default + DataStore impl behind `LuminineDependencies`, exactly like `SessionRepository`/`SurveyRepository`), exposed as a reactive `Flow<LuminineSettings>`. `App()` collects it and passes it to `LuminineTheme(settings)`, which resolves a light/dark/high-contrast `ColorScheme` (pure function) and applies a font-scale multiplier via `LocalDensity`. The WebView is an `expect`/`actual` `@Composable` (Android `WebView`, iOS `WKWebView`) with a common `WebViewController`; reading mode injects CSS. Navigation stays hand-rolled: `MainTab` gains `Shop`; 메뉴/마이페이지/설정/WebView open via an `overlay` state in `MainScaffold`.

**Tech Stack:** Kotlin Multiplatform 2.3.21, Compose Multiplatform 1.11.0, Material 3, AGP 9, kotlinx-serialization, kotlinx-coroutines (test pinned 1.9.0), androidx-datastore + okio.

**Build/test task names (verified, non-obvious — AGP-9 KMP renames):**
- Tests: `./gradlew :composeApp:testAndroidHostTest` (aggregate `:composeApp:allTests`). NOT `testDebugUnitTest`.
- Android compile/assemble: `./gradlew :androidApp:assembleDebug`. NOT `compileDebugKotlinAndroid`.
- iOS link: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` (+ `IosArm64`).
- A wrong task name makes gradle FAIL while a tail-piped exit code may read 0 — **always grep for `BUILD SUCCESSFUL`/`BUILD FAILED`**, never trust `$?` alone.

**Branch:** `feature/app-feature-update`, based on `feature/onboarding-survey-auth`.

---

## File Structure

**New (commonMain):**
- `model/Settings.kt` — `ThemeMode`, `FontScale`, `LuminineSettings`
- `data/settings/SettingsRepository.kt` — interface (`observe(): Flow`, `update{}`)
- `data/settings/InMemorySettingsRepository.kt`
- `data/store/DataStoreSettingsRepository.kt`
- `ui/theme/ColorSchemes.kt` — light/dark/HC schemes + pure `resolveColorScheme(...)`
- `ui/web/PlatformWebView.kt` — `expect` composable + `WebViewController` + `expect openInExternalBrowser` + `readerCss`
- `ui/screens/ShopScreen.kt`, `WebViewScreen.kt`, `MyPageScreen.kt`, `ReadabilitySettingsScreen.kt`, `MenuOverlay.kt`
- `content/HealthContent.kt`

**New (androidMain / iosMain):**
- `ui/web/PlatformWebView.android.kt`, `ui/web/PlatformWebView.ios.kt`

**Modified:**
- `di/LuminineDependencies.kt`, `App.kt`, `ui/theme/LuminineTheme.kt`, `ui/UiContent.kt`

**New tests (commonTest):**
- `model/SettingsTest.kt`, `data/settings/InMemorySettingsRepositoryTest.kt`, `ui/theme/ResolveColorSchemeTest.kt`,
  `ui/web/WebViewControllerTest.kt`, `ui/web/ReaderCssTest.kt`, `content/HealthContentTest.kt`,
  extend `ui/UiContentTest.kt`

---

## Phase 1 — Settings model & persistence (pure + repo)

### Task 1: Settings model

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/model/Settings.kt`
- Test: `composeApp/src/commonTest/kotlin/com/luminine/app/model/SettingsTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.luminine.app.model

import com.luminine.app.di.LuminineJson
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsTest {
    @Test
    fun defaultsAreSystemNoContrastNormal() {
        val s = LuminineSettings()
        assertEquals(ThemeMode.System, s.themeMode)
        assertEquals(false, s.highContrast)
        assertEquals(FontScale.Normal, s.fontScale)
    }

    @Test
    fun fontScaleMultipliers() {
        assertEquals(0.85f, FontScale.Small.multiplier)
        assertEquals(1.0f, FontScale.Normal.multiplier)
        assertEquals(1.25f, FontScale.Large.multiplier)
        assertEquals(1.5f, FontScale.ExtraLarge.multiplier)
    }

    @Test
    fun jsonRoundTrips() {
        val s = LuminineSettings(ThemeMode.Dark, highContrast = true, fontScale = FontScale.Large)
        val raw = LuminineJson.encodeToString(LuminineSettings.serializer(), s)
        assertEquals(s, LuminineJson.decodeFromString(LuminineSettings.serializer(), raw))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.model.SettingsTest"`
Expected: BUILD FAILED — `Settings.kt` types unresolved.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.luminine.app.model

import kotlinx.serialization.Serializable

enum class ThemeMode { System, Light, Dark }

enum class FontScale(val multiplier: Float) {
    Small(0.85f),
    Normal(1.0f),
    Large(1.25f),
    ExtraLarge(1.5f),
}

@Serializable
data class LuminineSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val highContrast: Boolean = false,
    val fontScale: FontScale = FontScale.Normal,
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.model.SettingsTest"`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/model/Settings.kt \
        composeApp/src/commonTest/kotlin/com/luminine/app/model/SettingsTest.kt
git commit -m "feat: LuminineSettings model (theme mode, high-contrast, font scale)"
```

### Task 2: SettingsRepository interface + in-memory impl

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/data/settings/SettingsRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/data/settings/InMemorySettingsRepository.kt`
- Test: `composeApp/src/commonTest/kotlin/com/luminine/app/data/settings/InMemorySettingsRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.luminine.app.data.settings

import com.luminine.app.model.FontScale
import com.luminine.app.model.LuminineSettings
import com.luminine.app.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemorySettingsRepositoryTest {
    @Test
    fun observeStartsWithDefaults() = runTest {
        assertEquals(LuminineSettings(), InMemorySettingsRepository().observe().first())
    }

    @Test
    fun updateMutatesAndEmits() = runTest {
        val repo = InMemorySettingsRepository()
        repo.update { it.copy(themeMode = ThemeMode.Dark, fontScale = FontScale.Large) }
        val s = repo.observe().first()
        assertEquals(ThemeMode.Dark, s.themeMode)
        assertEquals(FontScale.Large, s.fontScale)
    }

    @Test
    fun updateComposesOverPriorState() = runTest {
        val repo = InMemorySettingsRepository()
        repo.update { it.copy(highContrast = true) }
        repo.update { it.copy(fontScale = FontScale.ExtraLarge) }
        val s = repo.observe().first()
        assertEquals(true, s.highContrast)
        assertEquals(FontScale.ExtraLarge, s.fontScale)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.data.settings.InMemorySettingsRepositoryTest"`
Expected: BUILD FAILED — repository types unresolved.

- [ ] **Step 3: Write minimal implementation**

`SettingsRepository.kt`:
```kotlin
package com.luminine.app.data.settings

import com.luminine.app.model.LuminineSettings
import kotlinx.coroutines.flow.Flow

// Reactive read so theme/font changes recompose the whole app live. update() applies a transform
// over the latest value (read-modify-write), mirroring DataStore's edit{} semantics.
interface SettingsRepository {
    fun observe(): Flow<LuminineSettings>
    suspend fun update(transform: (LuminineSettings) -> LuminineSettings)
}
```

`InMemorySettingsRepository.kt`:
```kotlin
package com.luminine.app.data.settings

import com.luminine.app.model.LuminineSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Compile-safe default used by LuminineDependencies before platform startup swaps in DataStore.
class InMemorySettingsRepository(initial: LuminineSettings = LuminineSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override fun observe(): Flow<LuminineSettings> = state
    override suspend fun update(transform: (LuminineSettings) -> LuminineSettings) {
        state.update(transform)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.data.settings.InMemorySettingsRepositoryTest"`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/data/settings/ \
        composeApp/src/commonTest/kotlin/com/luminine/app/data/settings/
git commit -m "feat: SettingsRepository interface + in-memory impl"
```

### Task 3: DataStore-backed SettingsRepository

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/data/store/DataStoreSettingsRepository.kt`

(No new common test: DataStore needs a platform file path, so this impl is exercised via the on-device build, mirroring `DataStoreSessionRepository` which also has no commonTest. The in-memory test above covers the interface contract.)

- [ ] **Step 1: Write the implementation**

```kotlin
package com.luminine.app.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.luminine.app.data.settings.SettingsRepository
import com.luminine.app.di.LuminineJson
import com.luminine.app.model.LuminineSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = LuminineJson,
) : SettingsRepository {
    private val key = stringPreferencesKey("settings_json")

    override fun observe(): Flow<LuminineSettings> = dataStore.data.map { prefs ->
        decode(prefs[key])
    }

    override suspend fun update(transform: (LuminineSettings) -> LuminineSettings) {
        dataStore.edit { prefs ->
            val current = decode(prefs[key])
            prefs[key] = json.encodeToString(LuminineSettings.serializer(), transform(current))
        }
    }

    // Corrupt/old JSON falls back to defaults, never crashes (matches DataStoreSessionRepository).
    private fun decode(raw: String?): LuminineSettings =
        raw?.let { runCatching { json.decodeFromString(LuminineSettings.serializer(), it) }.getOrNull() }
            ?: LuminineSettings()
}
```

- [ ] **Step 2: Verify it compiles (common)**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/data/store/DataStoreSettingsRepository.kt
git commit -m "feat: DataStore-backed SettingsRepository"
```

### Task 4: Wire settings repo into DI

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/luminine/app/di/LuminineDependencies.kt`

- [ ] **Step 1: Add the imports**

Add to the import block:
```kotlin
import com.luminine.app.data.settings.InMemorySettingsRepository
import com.luminine.app.data.settings.SettingsRepository
import com.luminine.app.data.store.DataStoreSettingsRepository
```

- [ ] **Step 2: Add the property** (after the `surveyRepository` property, before `private var dataStoreInstalled`)

```kotlin
    var settingsRepository: SettingsRepository = InMemorySettingsRepository()
        private set
```

- [ ] **Step 3: Install in `installDataStore()`** (inside the existing body, after `surveyRepository = ...`)

```kotlin
        settingsRepository = DataStoreSettingsRepository(store)
```

- [ ] **Step 4: Extend the `override()` test hook**

Change the signature and body to include settings:
```kotlin
    fun override(
        kakao: KakaoAuthClient = kakaoAuthClient,
        session: SessionRepository = sessionRepository,
        survey: SurveyRepository = surveyRepository,
        settings: SettingsRepository = settingsRepository,
    ) {
        kakaoAuthClient = kakao
        sessionRepository = session
        surveyRepository = survey
        settingsRepository = settings
    }
```

- [ ] **Step 5: Verify compile + existing tests still pass**

Run: `./gradlew :composeApp:testAndroidHostTest`
Expected: BUILD SUCCESSFUL (all prior tests + the two new repo/model suites).

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/di/LuminineDependencies.kt
git commit -m "feat: register settingsRepository in LuminineDependencies"
```

---

## Phase 2 — Theme: color schemes + font scale

### Task 5: resolveColorScheme (pure)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/ui/theme/ColorSchemes.kt`
- Test: `composeApp/src/commonTest/kotlin/com/luminine/app/ui/theme/ResolveColorSchemeTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.luminine.app.ui.theme

import com.luminine.app.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertSame

class ResolveColorSchemeTest {
    @Test
    fun highContrastWinsRegardlessOfMode() {
        assertSame(HighContrastColorScheme, resolveColorScheme(ThemeMode.Light, highContrast = true, systemDark = false))
        assertSame(HighContrastColorScheme, resolveColorScheme(ThemeMode.Dark, highContrast = true, systemDark = true))
        assertSame(HighContrastColorScheme, resolveColorScheme(ThemeMode.System, highContrast = true, systemDark = true))
    }

    @Test
    fun lightAndDarkModesAreExplicit() {
        assertSame(LightColorScheme, resolveColorScheme(ThemeMode.Light, highContrast = false, systemDark = true))
        assertSame(DarkColorScheme, resolveColorScheme(ThemeMode.Dark, highContrast = false, systemDark = false))
    }

    @Test
    fun systemModeFollowsSystemDark() {
        assertSame(DarkColorScheme, resolveColorScheme(ThemeMode.System, highContrast = false, systemDark = true))
        assertSame(LightColorScheme, resolveColorScheme(ThemeMode.System, highContrast = false, systemDark = false))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.ui.theme.ResolveColorSchemeTest"`
Expected: BUILD FAILED — schemes/function unresolved.

- [ ] **Step 3: Write minimal implementation**

Move the existing `reverseColorScheme` content into `LightColorScheme` here, add dark + HC, and the resolver. (The existing private `reverseColorScheme` in `LuminineTheme.kt` is removed in Task 6.)

```kotlin
package com.luminine.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.luminine.app.model.ThemeMode

// Light = the existing ivory/gold brand scheme.
val LightColorScheme: ColorScheme = lightColorScheme(
    primary = ReverseGold,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D9C2),
    onPrimaryContainer = ReverseEspresso,
    secondary = ReverseGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCECE6),
    onSecondaryContainer = Color(0xFF143D33),
    tertiary = ReverseCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3D5CC),
    onTertiaryContainer = Color(0xFF5B241A),
    background = ReverseIvory,
    onBackground = ReverseInk,
    surface = Color.White,
    onSurface = ReverseInk,
    surfaceVariant = Color(0xFFE9E1D7),
    onSurfaceVariant = Color(0xFF5C5249),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFECE3D8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF8F4),
    surfaceContainer = Color(0xFFF5EEE6),
    surfaceContainerHigh = Color(0xFFEDE4DA),
    surfaceContainerHighest = Color(0xFFE6DCD1),
    surfaceTint = ReverseGold,
    inverseSurface = ReverseEspresso,
    inverseOnSurface = ReverseIvory,
    inversePrimary = Color(0xFFD5BE9A),
    outline = Color(0xFFB8A99A),
    outlineVariant = Color(0xFFD8CABC),
)

// Dark = warm espresso surfaces, gold accents.
val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFD5BE9A),
    onPrimary = Color(0xFF3A2C1E),
    primaryContainer = Color(0xFF5B4632),
    onPrimaryContainer = Color(0xFFF1E5D2),
    secondary = Color(0xFF8FCBBA),
    onSecondary = Color(0xFF0E3329),
    secondaryContainer = Color(0xFF234B40),
    onSecondaryContainer = Color(0xFFD4ECE4),
    tertiary = Color(0xFFE9A593),
    onTertiary = Color(0xFF54231A),
    background = Color(0xFF1A1714),
    onBackground = Color(0xFFEDE4DA),
    surface = Color(0xFF211D19),
    onSurface = Color(0xFFEDE4DA),
    surfaceVariant = Color(0xFF463F38),
    onSurfaceVariant = Color(0xFFD0C5B8),
    surfaceContainerLowest = Color(0xFF15120F),
    surfaceContainerLow = Color(0xFF211D19),
    surfaceContainer = Color(0xFF262220),
    surfaceContainerHigh = Color(0xFF312C28),
    surfaceContainerHighest = Color(0xFF3C3733),
    surfaceTint = Color(0xFFD5BE9A),
    outline = Color(0xFF978A7C),
    outlineVariant = Color(0xFF4C453E),
)

// High contrast = pure black on white, max-contrast borders. Independent of light/dark.
val HighContrastColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF000000),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF00332B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF000000),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFF6A0F00),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF2F2F2),
    surfaceContainerHighest = Color(0xFFE6E6E6),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF000000),
)

// Pure resolver — high contrast overrides everything; System follows the OS dark flag.
fun resolveColorScheme(mode: ThemeMode, highContrast: Boolean, systemDark: Boolean): ColorScheme {
    if (highContrast) return HighContrastColorScheme
    return when (mode) {
        ThemeMode.Light -> LightColorScheme
        ThemeMode.Dark -> DarkColorScheme
        ThemeMode.System -> if (systemDark) DarkColorScheme else LightColorScheme
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.ui.theme.ResolveColorSchemeTest"`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/ui/theme/ColorSchemes.kt \
        composeApp/src/commonTest/kotlin/com/luminine/app/ui/theme/ResolveColorSchemeTest.kt
git commit -m "feat: light/dark/high-contrast color schemes + pure resolveColorScheme"
```

### Task 6: LuminineTheme takes settings, applies scheme + font scale

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/luminine/app/ui/theme/LuminineTheme.kt`

- [ ] **Step 1: Replace the file body**

Remove the private `reverseColorScheme` (now `LightColorScheme` in `ColorSchemes.kt`) and rewrite the composable to accept settings, resolve the scheme, and scale fonts via `LocalDensity`.

```kotlin
package com.luminine.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.luminine.app.model.LuminineSettings

// Brand color tokens (kept here; schemes live in ColorSchemes.kt).
import androidx.compose.ui.graphics.Color
val ReverseIvory = Color(0xFFF7F3EE)
val ReverseGold = Color(0xFF8A7355)
val ReverseEspresso = Color(0xFF4B3628)
val ReverseGreen = Color(0xFF2D7D68)
val ReverseCoral = Color(0xFFC05A47)
val ReverseInk = Color(0xFF24211F)

@Composable
fun LuminineTheme(
    settings: LuminineSettings = LuminineSettings(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val scheme = resolveColorScheme(settings.themeMode, settings.highContrast, systemDark)
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * settings.fontScale.multiplier,
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography,
    ) {
        CompositionLocalProvider(LocalDensity provides scaledDensity, content = content)
    }
}
```

Note: the `Reverse*` token vals move here from the old file (they were already in this file). Keeping them here means `ColorSchemes.kt` and `App.kt` continue to import `com.luminine.app.ui.theme.ReverseGold` etc. unchanged.

- [ ] **Step 2: Verify compile**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD FAILED — `App.kt` still calls `LuminineTheme { ... }` without settings is fine (settings has a default), but the old `reverseColorScheme` reference is gone. Confirm the only errors (if any) are unrelated; the default-arg keeps the existing `LuminineTheme { }` call site valid. If it compiles, proceed.

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/ui/theme/LuminineTheme.kt
git commit -m "feat: LuminineTheme applies settings scheme + font scale via LocalDensity"
```

### Task 7: App() collects settings and feeds the theme

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/luminine/app/App.kt:126-132` (the top of `fun App()`)

- [ ] **Step 1: Add imports** (top import block)

```kotlin
import androidx.compose.runtime.collectAsState
import com.luminine.app.model.LuminineSettings
```

- [ ] **Step 2: Collect settings and pass to theme**

Replace the opening of `App()`:
```kotlin
@Composable
fun App() {
    LuminineTheme {
```
with:
```kotlin
@Composable
fun App() {
    val settingsRepo = remember { LuminineDependencies.settingsRepository }
    val settings by settingsRepo.observe().collectAsState(initial = LuminineSettings())
    LuminineTheme(settings) {
```

(`remember`, `getValue`, `rememberCoroutineScope`, `LaunchedEffect`, `mutableStateOf` are already imported.)

- [ ] **Step 3: Verify compile + tests**

Run: `./gradlew :composeApp:testAndroidHostTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/App.kt
git commit -m "feat: App collects settings flow and drives the theme"
```

---

## Phase 3 — Navigation: Shop tab + top-bar menu/mypage + overlay routing

### Task 8: Nav destinations — drop 메뉴, add Shop (with alignment test)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/luminine/app/ui/UiContent.kt:46-52`
- Test: `composeApp/src/commonTest/kotlin/com/luminine/app/ui/UiContentTest.kt`

- [ ] **Step 1: Read the current test, then add failing assertions**

Append to `UiContentTest.kt` (inside the existing test class):
```kotlin
    @Test
    fun topLevelDestinationsEndWithShopAndDropMenu() {
        val labels = topLevelDestinations().map { it.label }
        assertEquals(listOf("홈", "차트", "건강정보", "1:1케어", "Shop"), labels)
        assertTrue("Shop" in labels)
        assertTrue("메뉴" !in labels)
    }
```
Add imports if missing: `import kotlin.test.assertTrue`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.ui.UiContentTest"`
Expected: BUILD FAILED / assertion error — list still contains 메뉴 and ends differently.

- [ ] **Step 3: Update `topLevelDestinations()`**

Replace the list body with:
```kotlin
fun topLevelDestinations(): List<IconLabel> = listOf(
    IconLabel("홈", LuminineIcon.Home, "오늘의 루틴"),
    IconLabel("차트", LuminineIcon.Chart, "기록 차트"),
    IconLabel("건강정보", LuminineIcon.Book, "건강 정보"),
    IconLabel("1:1케어", LuminineIcon.Care, "일대일 케어"),
    IconLabel("Shop", LuminineIcon.Shop, "루미닌 숍"),
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.ui.UiContentTest"`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/ui/UiContent.kt \
        composeApp/src/commonTest/kotlin/com/luminine/app/ui/UiContentTest.kt
git commit -m "feat: bottom nav — drop 메뉴, append Shop (index-aligned with MainTab)"
```

### Task 9: ShopScreen (placeholder commerce grid)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/ui/screens/ShopScreen.kt`

(UI-only placeholder; no unit test — visual surface verified in the build/run step.)

- [ ] **Step 1: Write the screen**

```kotlin
package com.luminine.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luminine.app.ui.LuminineIcon
import com.luminine.app.ui.components.IconTile

private data class ShopProduct(val name: String, val price: String, val tag: String)

private val sampleProducts = listOf(
    ShopProduct("비타민C 1000 세럼", "₩38,000", "스킨케어"),
    ShopProduct("콜라겐 펩타이드", "₩45,000", "이너케어"),
    ShopProduct("오메가3 트리글리세라이드", "₩32,000", "영양제"),
    ShopProduct("저분자 단백질", "₩52,000", "식단"),
    ShopProduct("나이트 리커버리 크림", "₩41,000", "스킨케어"),
    ShopProduct("마그네슘 글리시네이트", "₩28,000", "수면"),
)

@Composable
fun ShopScreen(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(sampleProducts) { product ->
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconTile(
                            LuminineIcon.Shop,
                            product.name,
                            size = 56.dp,
                            background = MaterialTheme.colorScheme.secondaryContainer,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(product.tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(product.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(product.price, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/ui/screens/ShopScreen.kt
git commit -m "feat: placeholder ShopScreen commerce grid"
```

### Task 10: Add Shop tab branch + overlay state + top-bar 메뉴/마이페이지

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/luminine/app/App.kt` — `MainTab` enum (`109-115`), `MainScaffold` (`202-315`)

- [ ] **Step 1: Add `Shop` to `MainTab`**

```kotlin
private enum class MainTab(val label: String) {
    Home("홈"),
    Charts("차트"),
    Health("건강정보"),
    Care("1:1케어"),
    Shop("Shop"),
}
```

- [ ] **Step 2: Add an overlay state type** (near `RootState`, top-level private)

```kotlin
private sealed interface Overlay {
    data object None : Overlay
    data object Menu : Overlay
    data object MyPage : Overlay
    data object Readability : Overlay
    data class Web(val url: String, val title: String) : Overlay
}
```

- [ ] **Step 3: Add overlay state + settings repo handle inside `MainScaffold`** (after `var isAdmin by ...`)

```kotlin
        var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
        val scope = rememberCoroutineScope()
        val settingsRepo = remember { LuminineDependencies.settingsRepository }
```

- [ ] **Step 4: Replace the TopAppBar `actions` block** (currently the admin TextButton + Avatar) with menu + mypage icon buttons that keep the admin toggle:

```kotlin
                    actions = {
                        TextButton(onClick = { isAdmin = !isAdmin }) {
                            LuminineIconView(
                                icon = if (isAdmin) LuminineIcon.User else LuminineIcon.Admin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isAdmin) "회원" else "관리자")
                        }
                        IconButton(onClick = { overlay = Overlay.Menu }) {
                            LuminineIconView(LuminineIcon.Menu, "전체 메뉴", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { overlay = Overlay.MyPage }) {
                            LuminineIconView(LuminineIcon.User, "마이페이지", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.width(8.dp))
                    },
```
Add import: `import androidx.compose.material3.IconButton`.

- [ ] **Step 5: Add the Shop branch** in `when (selectedTab)` (after `MainTab.Care -> ...`, replacing the removed `MainTab.Menu` line):

```kotlin
                    MainTab.Shop -> ShopScreen(Modifier.padding(padding))
```
Remove the old `MainTab.Menu -> MenuScreen(...)` line. Add imports:
```kotlin
import com.luminine.app.ui.screens.ShopScreen
import com.luminine.app.ui.screens.MenuOverlay
import com.luminine.app.ui.screens.MyPageScreen
import com.luminine.app.ui.screens.ReadabilitySettingsScreen
import com.luminine.app.ui.screens.WebViewScreen
```

- [ ] **Step 6: Render the overlay** — wrap the existing `Scaffold(...) { padding -> ... }` content so overlays draw on top. After the closing brace of the `Scaffold` content lambda but still inside `MainScaffold`, add:

```kotlin
        when (val ov = overlay) {
            Overlay.None -> Unit
            Overlay.Menu -> MenuOverlay(
                displayName = session.displayName,
                records = records,
                survey = survey,
                onOpenReadability = { overlay = Overlay.Readability },
                onLogout = onLogout,
                onClose = { overlay = Overlay.None },
            )
            Overlay.MyPage -> MyPageScreen(
                displayName = session.displayName,
                onOpenReadability = { overlay = Overlay.Readability },
                onClose = { overlay = Overlay.None },
            )
            Overlay.Readability -> ReadabilitySettingsScreen(
                settingsRepo = settingsRepo,
                onClose = { overlay = Overlay.None },
            )
            is Overlay.Web -> WebViewScreen(
                url = ov.url,
                title = ov.title,
                onClose = { overlay = Overlay.None },
            )
        }
```

(Note: `MenuOverlay` is the extracted former `MenuScreen` body — Task 11. `MenuScreen`'s old signature was `MenuScreen(modifier, records, displayName, survey, onLogout)`.)

- [ ] **Step 7: Verify compile fails only on not-yet-created screens**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD FAILED — unresolved `MenuOverlay`, `MyPageScreen`, `ReadabilitySettingsScreen`, `WebViewScreen`. (Created in Tasks 11–14.) This is expected; do NOT commit yet. Proceed to Task 11.

### Task 11: Extract MenuOverlay from MenuScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/ui/screens/MenuOverlay.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/luminine/app/App.kt` — remove the old `private fun MenuScreen(...)` (`991-...`)

- [ ] **Step 1: Read the existing `MenuScreen` body** (`App.kt:991` onward) to copy its content verbatim.

Run: (inspect) the engineer reads `MenuScreen` and any private helpers it uses.

- [ ] **Step 2: Create `MenuOverlay.kt`**

A full-screen overlay (Surface covering the scaffold) with a top close/back row, the migrated menu content, plus an entry row to readability settings. Skeleton:

```kotlin
package com.luminine.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luminine.app.model.DailyRecord
import com.luminine.app.model.SurveyResponse

@Composable
fun MenuOverlay(
    displayName: String,
    records: List<DailyRecord>,
    survey: SurveyResponse?,
    onOpenReadability: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("닫기") }
                Text("전체 메뉴", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }
            // MIGRATED: paste the body previously rendered by MenuScreen here (points card,
            // Luminine service links). Use `displayName`, `records`, `survey` as before.
            MenuLink(label = "화면/가독성 설정", onClick = onOpenReadability)
            MenuLink(label = "로그아웃", onClick = onLogout)
        }
    }
}

@Composable
private fun MenuLink(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(label) }
}
```

The engineer pastes the real MenuScreen content (points, service links) into the marked region, adapting `Modifier.padding(padding)` to the overlay's own padding. Any private helpers used only by MenuScreen move here too.

- [ ] **Step 3: Delete the old `MenuScreen` from `App.kt`** (its content now lives in `MenuOverlay`; the `MainTab.Menu` branch was already removed in Task 10).

- [ ] **Step 4: Verify compile** (still expects MyPage/Readability/WebView)

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: BUILD FAILED — only `MyPageScreen`, `ReadabilitySettingsScreen`, `WebViewScreen` unresolved.

- [ ] **Step 5: Commit** (after Task 14 makes it compile; see Task 14 Step 4). For now, do not commit a broken tree — continue.

### Task 12: HealthContent URL seam

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/content/HealthContent.kt`
- Test: `composeApp/src/commonTest/kotlin/com/luminine/app/content/HealthContentTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.luminine.app.content

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class HealthContentTest {
    @Test
    fun everyTopicMapsToAnHttpsUrl() {
        HealthTopicKey.entries.forEach { key ->
            assertTrue(HealthContent.urlFor(key).startsWith("https://"), "url for $key")
        }
    }

    @Test
    fun urlsAreDistinctPerTopic() {
        val urls = HealthTopicKey.entries.map { HealthContent.urlFor(it) }
        assertEquals(urls.size, urls.toSet().size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.content.HealthContentTest"`
Expected: BUILD FAILED — `HealthContent`/`HealthTopicKey` unresolved.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.luminine.app.content

// Stable keys for health-info topics. Placeholder URLs this phase; swap to real content URLs later
// by editing urlFor() only — the single integration seam (README "next-phase boundary").
enum class HealthTopicKey { OxidativeStress, VitaminC, ExerciseScience, SleepAging, ResearchWiki }

object HealthContent {
    private const val BASE = "https://luminine.example.com/health"

    fun urlFor(key: HealthTopicKey): String = when (key) {
        HealthTopicKey.OxidativeStress -> "$BASE/oxidative-stress"
        HealthTopicKey.VitaminC -> "$BASE/vitamin-c"
        HealthTopicKey.ExerciseScience -> "$BASE/exercise-science"
        HealthTopicKey.SleepAging -> "$BASE/sleep-aging"
        HealthTopicKey.ResearchWiki -> "$BASE/research-wiki"
    }

    fun titleFor(key: HealthTopicKey): String = when (key) {
        HealthTopicKey.OxidativeStress -> "산화 스트레스"
        HealthTopicKey.VitaminC -> "비타민C"
        HealthTopicKey.ExerciseScience -> "운동 과학"
        HealthTopicKey.SleepAging -> "수면과 노화"
        HealthTopicKey.ResearchWiki -> "연구 백과"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.content.HealthContentTest"`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/content/HealthContent.kt \
        composeApp/src/commonTest/kotlin/com/luminine/app/content/HealthContentTest.kt
git commit -m "feat: HealthContent topic→URL seam (placeholder URLs)"
```

---

## Phase 4 — WebView (expect/actual) + reading mode

### Task 13: WebViewController + readerCss (pure, common) + expect declarations

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/ui/web/PlatformWebView.kt`
- Test: `composeApp/src/commonTest/kotlin/com/luminine/app/ui/web/WebViewControllerTest.kt`
- Test: `composeApp/src/commonTest/kotlin/com/luminine/app/ui/web/ReaderCssTest.kt`

- [ ] **Step 1: Write the failing tests**

`WebViewControllerTest.kt`:
```kotlin
package com.luminine.app.ui.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebViewControllerTest {
    @Test
    fun startsWithNoHistory() {
        val c = WebViewController()
        assertFalse(c.canGoBack)
        assertFalse(c.canGoForward)
    }

    @Test
    fun navStateReflectsLastReportedValues() {
        val c = WebViewController()
        c.onNavStateChanged(canGoBack = true, canGoForward = false)
        assertTrue(c.canGoBack)
        assertFalse(c.canGoForward)
    }

    @Test
    fun actionsInvokeBoundHandlers() {
        val c = WebViewController()
        val calls = mutableListOf<String>()
        c.bind(back = { calls += "back" }, forward = { calls += "forward" }, reload = { calls += "reload" })
        c.goBack(); c.goForward(); c.reload()
        assertEquals(listOf("back", "forward", "reload"), calls)
    }

    @Test
    fun actionsAreNoOpsBeforeBinding() {
        WebViewController().goBack() // must not throw
    }
}
```

`ReaderCssTest.kt`:
```kotlin
package com.luminine.app.ui.web

import com.luminine.app.model.FontScale
import kotlin.test.Test
import kotlin.test.assertTrue

class ReaderCssTest {
    @Test
    fun containsWidenedLineHeightAndHidesChrome() {
        val css = readerCss(FontScale.Normal)
        assertTrue("line-height" in css)
        assertTrue("display:none" in css.replace(" ", ""))
    }

    @Test
    fun fontScaleAffectsBaseFontSize() {
        val small = readerCss(FontScale.Small)
        val large = readerCss(FontScale.ExtraLarge)
        // larger scale yields a larger px value than smaller scale
        fun px(s: String) = Regex("font-size:\\s*(\\d+)px").find(s)!!.groupValues[1].toInt()
        assertTrue(px(large) > px(small))
    }

    @Test
    fun injectionUsesCreateTextNodeNotInnerHtml() {
        val js = readerInjectionJs(FontScale.Normal)
        // XSS-safe: builds the <style> via a text node, never assigns innerHTML.
        assertTrue("createTextNode" in js)
        assertTrue("innerHTML" !in js)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.ui.web.*"`
Expected: BUILD FAILED — types unresolved.

- [ ] **Step 3: Write the common implementation**

```kotlin
package com.luminine.app.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.luminine.app.model.FontScale

// Common controller shared by both platform WebViews. The actual WebView reports history changes
// via onNavStateChanged and binds its native back/forward/reload to the action handlers.
class WebViewController {
    var canGoBack by mutableStateOf(false)
        private set
    var canGoForward by mutableStateOf(false)
        private set

    private var back: () -> Unit = {}
    private var forward: () -> Unit = {}
    private var reload: () -> Unit = {}

    fun onNavStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        this.canGoBack = canGoBack
        this.canGoForward = canGoForward
    }

    fun bind(back: () -> Unit, forward: () -> Unit, reload: () -> Unit) {
        this.back = back
        this.forward = forward
        this.reload = reload
    }

    fun goBack() = back()
    fun goForward() = forward()
    fun reload() = reload()
}

// Reader stylesheet injected into the live page in reading mode. Base font honors the app font-scale
// so reading mode + the global font setting compose. Pure + testable.
fun readerCss(fontScale: FontScale): String {
    val basePx = (18 * fontScale.multiplier).toInt()
    return """
        header, footer, nav, aside, .ad, .ads { display:none !important; }
        body { max-width: 720px; margin: 0 auto; padding: 16px;
               line-height: 1.9 !important; font-size: ${basePx}px !important; }
        p, li { line-height: 1.9 !important; }
        img { max-width: 100%; height: auto; }
    """.trimIndent()
}

// JS that injects readerCss into the live page. Single source of truth for both platform actuals.
// XSS-safe by construction: the CSS is app-controlled and added via createTextNode (NOT innerHTML),
// and JSON-encoding the literal handles all string escaping. Pure + testable.
fun readerInjectionJs(fontScale: FontScale): String {
    // JsonPrimitive(...).toString() emits a valid, fully-escaped JS string literal (quotes + escapes).
    val cssLiteral = kotlinx.serialization.json.JsonPrimitive(readerCss(fontScale)).toString()
    return "(function(){var s=document.createElement('style');" +
        "s.appendChild(document.createTextNode($cssLiteral));" +
        "document.head.appendChild(s);})();"
}

// Renders a native WebView. readingMode toggles reader-CSS injection. controller carries nav state.
@Composable
expect fun PlatformWebView(
    url: String,
    readingMode: Boolean,
    controller: WebViewController,
    modifier: Modifier = Modifier,
)

// Opens the URL in the device's default browser (leaves the app).
expect fun openInExternalBrowser(url: String)
```

- [ ] **Step 4: Run tests to verify they pass** (controller + CSS; expect funcs have no actual yet — common metadata compile is fine, but the host-test target needs the Android actual. Create the Android actual in Task 15 first if the test target fails to link. To keep this task green standalone, the tests only touch `WebViewController` + `readerCss`, which are concrete.)

Run: `./gradlew :composeApp:testAndroidHostTest --tests "com.luminine.app.ui.web.*"`
Expected: BUILD SUCCESSFUL if the Android actual already exists; otherwise complete Task 15 then re-run. (Recommended: do Task 15 immediately after Step 3, before running.)

- [ ] **Step 5: Commit** (jointly with Task 15 actuals so the tree compiles)

### Task 14: WebViewScreen + ReadabilitySettingsScreen + MyPageScreen + Health integration

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/ui/screens/WebViewScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/ui/screens/ReadabilitySettingsScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/ui/screens/MyPageScreen.kt`
- Modify: `App.kt` `HealthInfoScreen` (`788`) to make topic cards open the WebView overlay

- [ ] **Step 1: WebViewScreen.kt**

```kotlin
package com.luminine.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luminine.app.ui.LuminineIcon
import com.luminine.app.ui.components.LuminineIconView
import com.luminine.app.ui.web.PlatformWebView
import com.luminine.app.ui.web.WebViewController
import com.luminine.app.ui.web.openInExternalBrowser

@Composable
fun WebViewScreen(url: String, title: String, onClose: () -> Unit) {
    val controller = remember { WebViewController() }
    var readingMode by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClose) { Text("닫기") }
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            PlatformWebView(url = url, readingMode = readingMode, controller = controller, modifier = Modifier.weight(1f).fillMaxWidth())
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { controller.goBack() }, enabled = controller.canGoBack) {
                    LuminineIconView(LuminineIcon.Link, "뒤로", Modifier.size(20.dp))
                }
                IconButton(onClick = { controller.goForward() }, enabled = controller.canGoForward) {
                    LuminineIconView(LuminineIcon.Link, "앞으로", Modifier.size(20.dp))
                }
                IconButton(onClick = { controller.reload() }) {
                    LuminineIconView(LuminineIcon.Link, "새로고침", Modifier.size(20.dp))
                }
                IconButton(onClick = { openInExternalBrowser(url) }) {
                    LuminineIconView(LuminineIcon.Link, "외부 브라우저로 열기", Modifier.size(20.dp))
                }
                FilledTonalButton(onClick = { readingMode = !readingMode }) {
                    Text(if (readingMode) "읽기 모드 끄기" else "읽기 모드")
                }
            }
        }
    }
}
```
Add import `androidx.compose.foundation.layout.size` and `androidx.compose.ui.unit.dp` (size on Modifier comes from `androidx.compose.foundation.layout.size`). (Note: icons reuse `LuminineIcon.Link` as a generic glyph; refine glyphs in a later polish pass.)

- [ ] **Step 2: ReadabilitySettingsScreen.kt** (theme/HC/font with live preview)

```kotlin
package com.luminine.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luminine.app.data.settings.SettingsRepository
import com.luminine.app.model.FontScale
import com.luminine.app.model.LuminineSettings
import com.luminine.app.model.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun ReadabilitySettingsScreen(settingsRepo: SettingsRepository, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by settingsRepo.observe().collectAsState(initial = LuminineSettings())
    fun mutate(transform: (LuminineSettings) -> LuminineSettings) {
        scope.launch { settingsRepo.update(transform) }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("닫기") }
                Text("화면/가독성 설정", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }

            // Live preview — re-renders at the current font scale via the app theme.
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("미리보기", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("오늘의 루틴", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("비타민C 1000mg · 아침 스트레칭 · 수분 섭취", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text("테마", fontWeight = FontWeight.SemiBold)
            ThemeMode.entries.forEach { mode ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = settings.themeMode == mode, onClick = { mutate { it.copy(themeMode = mode) } })
                    Text(
                        when (mode) {
                            ThemeMode.Light -> "라이트"
                            ThemeMode.Dark -> "다크"
                            ThemeMode.System -> "시스템"
                        },
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("고대비 모드", fontWeight = FontWeight.SemiBold)
                Switch(checked = settings.highContrast, onCheckedChange = { mutate { s -> s.copy(highContrast = it) } })
            }

            Text("글자 크기", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontScale.entries.forEach { fs ->
                    FilterChip(
                        selected = settings.fontScale == fs,
                        onClick = { mutate { it.copy(fontScale = fs) } },
                        label = {
                            Text(
                                when (fs) {
                                    FontScale.Small -> "작게"
                                    FontScale.Normal -> "보통"
                                    FontScale.Large -> "크게"
                                    FontScale.ExtraLarge -> "아주 크게"
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: MyPageScreen.kt**

```kotlin
package com.luminine.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MyPageScreen(displayName: String, onOpenReadability: () -> Unit, onClose: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("닫기") }
                Text("마이페이지", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("루미닌 회원", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onOpenReadability) { Text("화면/가독성 설정") }
        }
    }
}
```

- [ ] **Step 4: Make health topic cards open the WebView** — modify `HealthInfoScreen` to accept an `onOpenTopic` callback and wire `TopicCard` clicks.

In `App.kt`, change `HealthInfoScreen` signature and the `MainTab.Health` call site:
```kotlin
// signature
private fun HealthInfoScreen(modifier: Modifier, onOpenTopic: (HealthTopicKey) -> Unit) {
```
Map each `healthTopics()` card index to a `HealthTopicKey` (same order as `healthTopics()`):
`산화 스트레스→OxidativeStress, 비타민C→VitaminC, 운동 과학→ExerciseScience, 수면과 노화→SleepAging, 연구 백과→ResearchWiki`. Make `TopicCard` take an `onClick` and call `onOpenTopic(key)`.

Call site (`MainTab.Health`):
```kotlin
                    MainTab.Health -> HealthInfoScreen(Modifier.padding(padding)) { key ->
                        overlay = Overlay.Web(HealthContent.urlFor(key), HealthContent.titleFor(key))
                    }
```
Add imports: `import com.luminine.app.content.HealthContent` and `import com.luminine.app.content.HealthTopicKey`.

- [ ] **Step 5: Commit** (after Task 15 makes the actuals exist and the tree compiles)

### Task 15: Android + iOS PlatformWebView actuals + openInExternalBrowser

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/luminine/app/ui/web/PlatformWebView.android.kt`
- Create: `composeApp/src/iosMain/kotlin/com/luminine/app/ui/web/PlatformWebView.ios.kt`

- [ ] **Step 1: Android actual**

```kotlin
package com.luminine.app.ui.web

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.luminine.app.model.FontScale

@Composable
actual fun PlatformWebView(url: String, readingMode: Boolean, controller: WebViewController, modifier: Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, finishedUrl: String?) {
                        controller.onNavStateChanged(view.canGoBack(), view.canGoForward())
                        if (readingMode) view.evaluateJavascript(readerInjectionJs(FontScale.Normal), null)
                    }
                }
                controller.bind(
                    back = { if (canGoBack()) goBack() },
                    forward = { if (canGoForward()) goForward() },
                    reload = { reload() },
                )
                loadUrl(url)
            }
        },
        update = { view ->
            if (readingMode) view.evaluateJavascript(readerInjectionJs(FontScale.Normal), null) else view.reload()
        },
    )
}

private var appContextForBrowser: android.content.Context? = null

actual fun openInExternalBrowser(url: String) {
    val ctx = appContextForBrowser ?: return
    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

// Seeded from AndroidAppContext (reuse the existing platform context holder).
fun seedBrowserContext(context: android.content.Context) { appContextForBrowser = context.applicationContext }
```

Note: reuse the existing `platform.AndroidAppContext` instead of a new holder if it already exposes an application Context — the engineer checks `AndroidAppContext.kt` and wires `openInExternalBrowser` to it, removing `seedBrowserContext` if redundant. (The plan's intent: external-open uses the already-seeded app Context, not a fresh holder.)

- [ ] **Step 2: iOS actual**

```kotlin
package com.luminine.app.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.luminine.app.model.FontScale
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.WebKit.WKWebView

@Composable
actual fun PlatformWebView(url: String, readingMode: Boolean, controller: WebViewController, modifier: Modifier) {
    UIKitView(
        modifier = modifier,
        factory = {
            val web = WKWebView()
            controller.bind(
                back = { if (web.canGoBack) web.goBack() },
                forward = { if (web.canGoForward) web.goForward() },
                reload = { web.reload() },
            )
            NSURL.URLWithString(url)?.let { web.loadRequest(NSURLRequest(it)) }
            web
        },
        update = { web ->
            controller.onNavStateChanged(web.canGoBack, web.canGoForward)
            if (readingMode) {
                web.evaluateJavaScript(readerInjectionJs(FontScale.Normal), null)
            }
        },
    )
}

actual fun openInExternalBrowser(url: String) {
    NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it) }
}
```

- [ ] **Step 3: Wire Android external-browser context** — in `MainActivity.onCreate` (or via the existing `LumininePersistence.init`/`AndroidAppContext`), ensure the app Context used by `openInExternalBrowser` is seeded. Prefer reusing `AndroidAppContext`; only add a seed call if needed.

- [ ] **Step 4: Compile all targets + run full test suite**

Run: `./gradlew :composeApp:testAndroidHostTest`
Expected: BUILD SUCCESSFUL (all suites incl. web).

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit** (the Phase-3/4 wiring that depended on these actuals — Tasks 10, 11, 13, 14, 15 — now compiles together)

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/ui/web/ \
        composeApp/src/androidMain/kotlin/com/luminine/app/ui/web/ \
        composeApp/src/iosMain/kotlin/com/luminine/app/ui/web/ \
        composeApp/src/commonMain/kotlin/com/luminine/app/ui/screens/ \
        composeApp/src/commonTest/kotlin/com/luminine/app/ui/web/ \
        composeApp/src/commonMain/kotlin/com/luminine/app/App.kt
git commit -m "feat: in-app WebView (expect/actual) + reading mode + overlay screens"
```

---

## Phase 5 — Hardcoded color cleanup + final verification

### Task 16: Route visible hardcoded Reverse* colors through MaterialTheme

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/luminine/app/App.kt` — the nav tint and prominent icon tints that won't react to dark/HC.

- [ ] **Step 1: Find direct color uses in visible chrome**

Run: `grep -n "ReverseEspresso\|ReverseGold\|ReverseGreen\|ReverseCoral\|ReverseIvory\|Color.White\|ReverseInk" composeApp/src/commonMain/kotlin/com/luminine/app/App.kt`

- [ ] **Step 2: Replace theme-critical ones** — at minimum the bottom-nav selected tint (`ReverseEspresso` at the old `261`) and the top-bar/admin tints already changed in Task 10. Map:
  - selected nav tint → `MaterialTheme.colorScheme.primary`
  - `ReverseEspresso` text/labels in chrome → `MaterialTheme.colorScheme.onSurface`
  - leave card-accent decorative tints (e.g. TopicCard's green wash) as-is unless they become unreadable in dark — those use `.copy(alpha=…)` over surfaces and remain acceptable this pass.

Keep this targeted: chrome and high-traffic text, not every decorative accent.

- [ ] **Step 3: Verify compile + tests**

Run: `./gradlew :composeApp:testAndroidHostTest && ./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL for both.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/App.kt
git commit -m "refactor: route nav/chrome colors through MaterialTheme for dark/high-contrast"
```

### Task 17: Full verification sweep

- [ ] **Step 1: All tests**

Run: `./gradlew :composeApp:allTests`
Expected: `BUILD SUCCESSFUL`. Grep the output for `BUILD SUCCESSFUL` explicitly — do not trust the shell exit code alone.

- [ ] **Step 2: Android assemble**

Run: `./gradlew :androidApp:assembleDebug`
Expected: `BUILD SUCCESSFUL`; APK at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

- [ ] **Step 3: iOS link (both arches)**

Run: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:linkDebugFrameworkIosArm64`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual smoke (device/emulator)** — via the `/run` or `/qa` skill, confirm:
  - Bottom nav shows 홈·차트·건강정보·1:1케어·Shop; Shop renders the product grid.
  - Top-right ☰ opens 전체 메뉴; 👤 opens 마이페이지.
  - 마이페이지/메뉴 → 화면/가독성 설정: changing theme/HC/font updates the whole app live; survives app restart (DataStore).
  - A 건강정보 topic card opens the in-app WebView; back/forward/refresh/외부 브라우저/읽기 모드 all function; reading mode widens spacing.

- [ ] **Step 5: Update README** — extend the "Implemented MVP Surface" list with Shop tab, top-bar menu/mypage, in-app WebView + reading mode, and accessibility (font scale, dark, high-contrast). Note the placeholder content + `HealthContent` URL seam under the integration-boundaries paragraph.

```bash
git add README.md
git commit -m "docs: README — Shop, WebView, accessibility surface"
```

---

## Self-Review

**Spec coverage:**
- §1 nav/layout → Tasks 8 (destinations), 9 (Shop screen), 10 (tab branch + top-bar ☰/👤 + overlay), 11 (menu overlay). ✓
- §2 WebView → Tasks 13 (controller/css/expect), 14 (screen + health integration), 15 (actuals). Control bar (back/fwd/refresh/external) in Task 14 Step 1. ✓
- §3 accessibility → font scale (Tasks 1,6,7,14), dark + system sync (Tasks 5,6), high-contrast (Tasks 5,6,14), 화면/가독성 설정 screen (Task 14), reading mode (Tasks 13,14,15). ✓
- Persistence (DataStore SettingsRepository) → Tasks 2,3,4. ✓

**Placeholder scan:** Two intentional "MIGRATED: paste …" markers in Task 11 reference real existing code the engineer copies from `App.kt:991` (cited) — not a vague instruction; the surrounding skeleton is complete. The Android `openInExternalBrowser` notes reusing `AndroidAppContext` with a concrete fallback. No TODO/TBD left as functional gaps.

**Type consistency:** `WebViewController` methods (`onNavStateChanged`, `bind`, `goBack/goForward/reload`, `canGoBack/canGoForward`) consistent across Tasks 13/14/15. `SettingsRepository.observe()/update{}` consistent across Tasks 2/3/4/14. `HealthTopicKey`/`HealthContent.urlFor/titleFor` consistent Tasks 12/14. `resolveColorScheme(mode, highContrast, systemDark)` + `Light/Dark/HighContrastColorScheme` consistent Tasks 5/6. `LuminineTheme(settings, content)` consistent Tasks 6/7.

**Commit-ordering caveat (called out for the executor):** Tasks 10, 11, 13, 14 leave the tree non-compiling until Task 15 supplies the `expect`/`actual` actuals and the overlay screens exist. Commit those tasks **together** at Task 15 Step 5 (and Task 13 Step 5 folds in). All other tasks compile and commit independently. This is an intentional dependency cluster for the cross-platform seam, not an oversight.
