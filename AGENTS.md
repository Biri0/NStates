# AGENTS.md — NStates (NationStates Android Client)

## Project Overview

NStates is an Android client for the NationStates API, built with Kotlin and Jetpack
Compose (Material 3). Single-module Gradle project (`app`).

**Package:** `it.rfmariano.nstates`
**Min SDK:** 26 (Android 8.0) | **Target/Compile SDK:** 36
**Java compatibility:** 11
**Gradle:** 9.1.0 (Kotlin DSL, version catalog)

## Important: Do NOT Commit

**Never create git commits unless the user explicitly asks you to.** The user wants to
review all code changes before committing. Just make the edits and let the user know
what changed.

## Build Commands

```bash
# Full debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug

# Check (compile + lint + test)
./gradlew check

# Compile only (fast verification)
./gradlew compileDebugKotlin
```

## Test Commands

```bash
# Run ALL unit tests
./gradlew test

# Run unit tests for debug variant only
./gradlew testDebugUnitTest

# Run a SINGLE test class
./gradlew testDebugUnitTest --tests "it.rfmariano.nstates.ExampleUnitTest"

# Run a SINGLE test method
./gradlew testDebugUnitTest --tests "it.rfmariano.nstates.ExampleUnitTest.addition_isCorrect"

# Run tests matching a pattern
./gradlew testDebugUnitTest --tests "it.rfmariano.nstates.*"

# Run ALL instrumented (on-device/emulator) tests
./gradlew connectedDebugAndroidTest

# Run a SINGLE instrumented test class
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=it.rfmariano.nstates.ExampleInstrumentedTest
```

Test reports are generated at:
- Unit tests: `app/build/reports/tests/testDebugUnitTest/index.html`
- Instrumented: `app/build/reports/androidTests/connected/index.html`

## Lint

```bash
# Run Android lint
./gradlew lint
./gradlew lintDebug

# Lint report location
# app/build/reports/lint-results-debug.html
```

No detekt, ktlint, or spotless is configured. Only the built-in Android lint and
`kotlin.code.style=official` in `gradle.properties`.

## Project Structure

```
NStates/
├── build.gradle.kts              # Root build script (plugin declarations)
├── settings.gradle.kts           # Module includes, repositories
├── gradle.properties             # JVM args, AndroidX, Kotlin code style
├── gradle/libs.versions.toml     # Version catalog (all dependency versions)
└── app/
    ├── build.gradle.kts          # App build config, dependencies
    └── src/
        ├── main/java/it/rfmariano/nstates/
        │   ├── MainActivity.kt
        │   └── ui/theme/         # Color.kt, Theme.kt, Type.kt
        ├── main/res/             # Drawables, mipmaps, values, xml
        ├── test/                 # JUnit 4 unit tests
        └── androidTest/          # Instrumented tests (Espresso, Compose UI)
```

## Dependencies & Version Catalog

All dependency versions live in `gradle/libs.versions.toml`. When adding dependencies:
1. Add the version to `[versions]`
2. Add the library to `[libraries]` with `version.ref`
3. Reference in `app/build.gradle.kts` as `libs.your.library.name`

Current stack: Compose BOM 2024.09.00, Material 3, Lifecycle 2.10.0,
Activity Compose 1.12.3. No HTTP client, DI, navigation, or persistence yet.

## Code Style Guidelines

### Language & Formatting
- **Kotlin only** — no Java source files
- **4-space indentation**, no tabs
- Opening braces on the same line as the declaration
- Use **trailing lambdas** for Compose and DSL-style APIs
- Use **named arguments** for Composable function calls
- **No wildcard imports** in production code (`import foo.bar.Baz`, not `foo.bar.*`)
- Imports organized: Android SDK, AndroidX, third-party, project-internal
- One blank line between top-level declarations

### Naming Conventions
- **Packages:** lowercase dotted (`it.rfmariano.nstates.ui.theme`)
- **Classes/Objects:** PascalCase (`MainActivity`, `NationRepository`)
- **Composable functions:** PascalCase (`NationCard`, `IssueListScreen`)
- **Non-composable functions:** camelCase (`fetchNation`, `parseIssues`)
- **Properties/variables:** camelCase (`nationName`, `isLoading`)
- **Constants/top-level vals:** PascalCase for colors and theme values
  (`Purple80`, `DarkColorScheme`), UPPER_SNAKE_CASE for true constants
- **Test methods:** snake_case describing behavior (`login_withValidCredentials_succeeds`)
- **Preview functions:** suffixed with `Preview` (`NationCardPreview`)

### Compose Conventions
- Every Composable accepting layout configuration must take
  `modifier: Modifier = Modifier` as a parameter
- Always pass `modifier` as the **last non-trailing-lambda** parameter
- Root layout in screens should be `Scaffold` with `Modifier.fillMaxSize()`
- Pass `innerPadding` from Scaffold down to content
- Wrap all content in `NStatesTheme` at the Activity level
- Use `enableEdgeToEdge()` in Activity `onCreate`
- Previews use `@Preview(showBackground = true)` and wrap content in `NStatesTheme`

### Types
- Use explicit types on public API boundaries (function parameters, return types)
- Use type inference for local variables and private implementations
- Prefer `StateFlow` over `LiveData` for observable state
- Use `sealed class`/`sealed interface` for UI state modeling
- Prefer data classes for API response models

### Error Handling
- Use `Result<T>` or sealed classes for operations that can fail
- Never silently swallow exceptions — log or propagate
- Use `runCatching` for wrapping API calls
- Coroutine exception handlers at the ViewModel level
- Show user-facing errors via Compose state, not Toast/Snackbar imperatively

### Architecture (target patterns for this project)
- **MVVM** with Compose: Screen -> ViewModel -> Repository -> Data Source
- ViewModels expose `StateFlow<UiState>` collected in Composables
- Repositories are the single source of truth for data
- Suspend functions for one-shot operations, Flows for streams
- Keep Composables stateless where possible — hoist state to ViewModels

## Testing Guidelines

- **Unit tests** go in `app/src/test/` — use JUnit 4
- **Instrumented tests** go in `app/src/androidTest/` — use AndroidJUnit4 runner
- **Compose UI tests** use `createComposeRule()` from `ui-test-junit4`
- Test file naming: `{ClassName}Test.kt` (e.g., `NationViewModelTest.kt`)
- Test method naming: `methodOrBehavior_condition_expectedResult`
  (e.g., `fetchNation_networkError_showsErrorState`)
- Prefer fakes over mocks when possible

## Useful Notes

- The Gradle wrapper (`./gradlew`) must be used — do not rely on a system Gradle
- All commands should be run from the project root (`/home/student/AndroidStudioProjects/NStates`)
- No remote git repository is configured yet
- No CI/CD pipelines exist — validate changes locally with `./gradlew check`
