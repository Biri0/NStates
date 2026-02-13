# AGENTS.md — NStates (NationStates Android Client)

## Project Overview

Android client for the NationStates API, built with Kotlin + Jetpack Compose
(Material 3). Single-module Gradle project (`app`).

**Package:** `it.rfmariano.nstates`
**Min SDK:** 26 (Android 8.0) | **Target/Compile SDK:** 36
**Java compatibility:** 11
**Gradle:** 9.1.0 (Kotlin DSL, version catalog)

## Important: Do NOT Commit

Never create git commits unless the user explicitly asks you to. The user wants
to review all code changes before committing. Just make the edits and report
what changed.

## Mandatory Execution Rules (Agent Behavior)

1. **Run tests before ending**: always execute tests before your final response.
   Only run tests; do not compile or assemble. Goal: code remains compilable in
   release mode. If tests are too slow or require devices, say what you ran and
   why.
2. **When you have a suggestion or a next step**: do not stop writing. Use the
   question tool. Always ask what to do next (or ask the specific question you
   need). This applies to every response.

## NationStates API

The API reference is at `api_reference.md` in the project root. Use it whenever
implementing API features. Key points:
- Base URL: `https://www.nationstates.net/cgi-bin/api.cgi`
- XML responses parsed with `XmlPullParser` (no third-party XML library)
- Auth flow: `X-Password` -> receive `X-Pin` + `X-Autologin` -> use `X-Pin`
- Rate limit: 50 requests / 30 seconds; respect `RateLimit-Remaining` and
  `Retry-After` headers
- Every request must include a `User-Agent` header
- Private commands use a two-step prepare/execute flow with tokens

## Build Commands

```bash
./gradlew assembleDebug          # Full debug build
./gradlew assembleRelease        # Release build
./gradlew clean assembleDebug    # Clean build
./gradlew check                  # Compile + lint + test
./gradlew compileDebugKotlin     # Compile only (fast verification)
```

## Test Commands

```bash
./gradlew test                   # All unit tests
./gradlew testDebugUnitTest      # Unit tests (debug variant)

# Single test class
./gradlew testDebugUnitTest --tests "it.rfmariano.nstates.ExampleUnitTest"

# Single test method
./gradlew testDebugUnitTest --tests "it.rfmariano.nstates.ExampleUnitTest.addition_isCorrect"

# Pattern matching
./gradlew testDebugUnitTest --tests "it.rfmariano.nstates.*"

# Instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Single instrumented test class
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=it.rfmariano.nstates.ExampleInstrumentedTest
```

Test reports: `app/build/reports/tests/testDebugUnitTest/index.html`

## Lint

```bash
./gradlew lint                   # or ./gradlew lintDebug
# Report: app/build/reports/lint-results-debug.html
```

No detekt, ktlint, or spotless configured. Only built-in Android lint and
`kotlin.code.style=official` in `gradle.properties`.

## Project Structure

```
app/src/main/java/it/rfmariano/nstates/
├── NStatesApplication.kt              # @HiltAndroidApp
├── MainActivity.kt                    # Entry point + bottom nav
├── di/NetworkModule.kt                # Hilt: provides Ktor HttpClient
├── data/
│   ├── api/
│   │   ├── NationStatesApiClient.kt   # Low-level HTTP + rate limiting
│   │   ├── NationApi.kt              # Nation endpoint wrapper
│   │   ├── IssueApi.kt               # Issues endpoint wrapper
│   │   ├── NationXmlParser.kt        # XML -> NationData
│   │   ├── IssueXmlParser.kt         # XML -> Issues/IssueResult
│   │   ├── XmlSanitizer.kt           # HTML entity -> Unicode
│   │   └── CensusScales.kt           # Scale ID -> name lookup
│   ├── local/
│   │   ├── AuthLocalDataSource.kt    # EncryptedSharedPreferences
│   │   └── SettingsDataSource.kt     # DataStore preferences
│   ├── model/
│   │   ├── NationData.kt             # Nation data classes
│   │   ├── AuthModels.kt             # Auth + rate limit models
│   │   └── IssueModels.kt            # Issue, IssueResult, etc.
│   └── repository/
│       └── NationRepository.kt       # Single source of truth
└── ui/
    ├── theme/                         # Color.kt, Theme.kt, Type.kt
    ├── navigation/                    # Routes.kt, NStatesNavHost.kt
    ├── login/                         # LoginScreen, ViewModel, UiState
    ├── nation/                        # NationScreen, ViewModel, UiState
    ├── issues/                        # IssuesScreen, ViewModel, UiState
    └── settings/                      # SettingsScreen, ViewModel, UiState
```

## Dependencies & Version Catalog

All versions live in `gradle/libs.versions.toml`. When adding dependencies:
1. Add the version to `[versions]`
2. Add the library to `[libraries]` with `version.ref`
3. Reference in `app/build.gradle.kts` as `libs.your.library.name`

Current stack: Compose BOM 2026.01.01, Material 3, Lifecycle 2.10.0,
Activity Compose 1.12.3, Navigation Compose 2.9.7, Hilt 2.59.1, Ktor 3.4.0,
Coil 3.3.0, Room 2.8.4, DataStore 1.2.0, Security-Crypto 1.1.0.

## Code Style Guidelines

### Language & Formatting
- **Kotlin only** — no Java source files
- **4-space indentation**, no tabs; opening braces on same line
- **Trailing lambdas** for Compose and DSL-style APIs
- **Named arguments** for Composable function calls
- **No wildcard imports** (`import foo.bar.Baz`, not `foo.bar.*`)
- Imports ordered: Android SDK, AndroidX, third-party, project-internal
- Keep files focused: one public class per file when possible

### Imports & File Organization
- Group imports by top-level package with a blank line between groups
- Alphabetize imports within each group
- Avoid unused imports; prefer explicit imports over aliasing

### Naming Conventions
- **Packages:** lowercase dotted (`it.rfmariano.nstates.ui.theme`)
- **Classes/Objects:** PascalCase (`NationRepository`)
- **Composable functions:** PascalCase (`NationCard`, `IssueListScreen`)
- **Non-composable functions:** camelCase (`fetchNation`)
- **Properties/variables:** camelCase (`nationName`, `isLoading`)
- **Constants:** PascalCase for theme values, UPPER_SNAKE_CASE for true constants
- **Test methods:** `methodOrBehavior_condition_expectedResult`
- **Preview functions:** suffixed with `Preview` (`NationCardPreview`)

### Types & API Boundaries
- Explicit types on public API boundaries; inference for locals/privates
- Use `sealed class`/`sealed interface` for UI state
- Favor `Result<T>` or sealed result types for fallible operations
- Prefer immutable data classes; use `val` unless mutation is required

### Error Handling
- Never silently swallow exceptions — log or propagate
- `runCatching` for API calls when you must capture errors
- ViewModels handle errors and expose state; UI renders state only
- User-facing errors via Compose state, not imperative Toast/Snackbar
- Include actionable error messages for network and parsing failures

### Coroutines & Flows
- Use `StateFlow` for UI state; collect with lifecycle-aware APIs
- Repository methods are `suspend` for one-shot, `Flow` for streams
- Avoid `GlobalScope`; use `viewModelScope` or injected scopes
- Prefer structured concurrency and cancellation-friendly APIs

### Compose Conventions
- Composables accepting layout config take `modifier: Modifier = Modifier`
- Pass `modifier` as the last non-trailing-lambda parameter
- Root layout: `Scaffold` + `Modifier.fillMaxSize()`, apply `innerPadding`
- Wrap content in `NStatesTheme` at Activity level; use `enableEdgeToEdge()`
- Previews: `@Preview(showBackground = true)` wrapped in `NStatesTheme`

### Architecture
- **MVVM:** Composable -> ViewModel -> Repository -> API / Local Data Source
- ViewModels expose `StateFlow<UiState>` collected in Composables
- Repository is single source of truth; suspend for one-shot, Flow for streams
- Keep Composables stateless — hoist state to ViewModels
- All DI via Hilt (`@HiltViewModel`, `@Inject`, `@Singleton`)

## Testing Guidelines

- **Unit tests** in `app/src/test/` — JUnit 4
- **Instrumented tests** in `app/src/androidTest/` — AndroidJUnit4 runner
- **Compose UI tests** use `createComposeRule()` from `ui-test-junit4`
- File naming: `{ClassName}Test.kt`; prefer fakes over mocks
- Currently only placeholder tests exist — real tests are needed

## Notes for Agents

- Use `./gradlew` (wrapper), not system Gradle
- All commands run from project root
- No remote git repository configured; validate with `./gradlew check`
- UI strings are currently hardcoded in Kotlin (not externalized to strings.xml)

## Cursor and Copilot Rules

- No `.cursor/rules/`, `.cursorrules`, or `.github/copilot-instructions.md` found
  at the time of writing. If any are added, follow them.
