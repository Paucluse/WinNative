# Plan: LSFG Migration For WinNative

**Generated**: 2026-05-12
**Estimated Complexity**: High

## Overview

Migrate the LSFG-based frame generation path into `D:\Codex\Winnative-fork` by using three references together:

1. The latest `LSFG-Android` design as the upstream behavior reference.
2. `GameNative` as proof that LSFG-VK can be embedded into an Android PC-gaming app.
3. The older local `D:\Codex\Winnative` checkout as the most relevant migration source, because it already contains a partial WinNative-specific LSFG integration scaffold.

The implementation should prioritize safety first:

- LSFG must be disabled by default.
- The user-supplied `Lossless.dll` must never be bundled.
- The LSFG switch must be set before entering the game/session.
- If LSFG initialization, shader extraction, or runtime output fails, WinNative must continue to function normally without damaging the base game session.

## Prerequisites

- Local LSFG reference tree at `D:\Codex\LSFG-Android-ref`
- Local prior WinNative integration at `D:\Codex\Winnative`
- Target repo at `D:\Codex\Winnative-fork`
- User-provided `Lossless.dll` flow only; no redistribution
- Existing known-good baseline in `Winnative-fork` must preserve controller vibration behavior

## Sprint 1: Safe Migration Shell
**Goal**: Land the non-destructive LSFG shell in `Winnative-fork` without changing the default runtime behavior.

**Demo/Validation**:
- App builds successfully with LSFG code present
- Entering games with LSFG disabled behaves exactly like the current baseline
- No `Lossless.dll` is bundled into the APK

### Task 1.1: Port Java framegen package
- **Location**: `app/src/main/runtime/display/framegen/`
- **Description**: Migrate the Java-side LSFG support package from the old `Winnative` checkout:
  - `FrameGenerationBridge.java`
  - `FrameGenerationCaptureController.java`
  - `FrameGenerationConfig.java`
  - `LosslessDllManager.java`
  - `LosslessShaderExtractor.java`
- **Dependencies**: none
- **Acceptance Criteria**:
  - Files compile in `Winnative-fork`
  - No runtime path is enabled automatically yet
- **Validation**:
  - Project compile check
  - Search confirms package is present in target repo

### Task 1.2: Add pre-game LSFG kill switch and persisted config
- **Location**: likely `app/src/main/feature/settings/`, session drawer UI, and preference access sites
- **Description**: Add a persistent `LSFG` master toggle that is evaluated before game entry. Keep it disabled by default and require the user to turn it on explicitly before session start.
- **Dependencies**: Task 1.1
- **Acceptance Criteria**:
  - Preference exists and defaults to off
  - Base session launch path ignores LSFG when off
  - UI clearly communicates that LSFG should be set before entering a game
- **Validation**:
  - Manual code path audit
  - Launch flow with default preferences

### Task 1.3: Add user DLL import scaffolding only
- **Location**: session/settings UI and `LosslessDllManager`
- **Description**: Port the file-pick/import flow for `Lossless.dll`, but keep it non-fatal and decoupled from session launch.
- **Dependencies**: Task 1.1
- **Acceptance Criteria**:
  - User can import a DLL into private app storage
  - Missing DLL never blocks normal game launch
  - Imported DLL path is stored only in app-private files
- **Validation**:
  - Manual path inspection
  - Null/missing-DLL code path review

## Sprint 2: Native Bridge And Build Wiring
**Goal**: Bring the native LSFG bridge into the build in a way that still preserves fallback behavior.

**Demo/Validation**:
- Native code builds in `Winnative-fork`
- App can initialize or reject LSFG cleanly
- Failing initialization drops back to normal rendering

### Task 2.1: Port JNI bridge and native helper files
- **Location**:
  - `app/src/main/cpp/winlator/framegen_native_bridge.cpp`
  - `app/src/main/cpp/winlator/framegen_native_bridge.hpp`
  - `app/src/main/cpp/winlator/framegen_shader_bridge.cpp`
  - `app/src/main/cpp/winlator/lsfg_session.c`
  - `app/src/main/cpp/winlator/lsfg_session.h`
  - any required support files from the old checkout
- **Description**: Move the old native bridge into the target repo and identify which parts are scaffold-only versus which parts come directly from `LSFG-Android`.
- **Dependencies**: Sprint 1 complete enough to compile
- **Acceptance Criteria**:
  - JNI symbols match Java bridge expectations
  - Code compiles without breaking current native libraries
- **Validation**:
  - Native build
  - Symbol/link success during Gradle build

### Task 2.2: Reconcile CMake wiring with reference sources
- **Location**: `app/src/main/cpp/CMakeLists.txt`
- **Description**: Replace any brittle absolute-path assumptions from the old checkout with reproducible wiring suitable for `Winnative-fork`. Decide whether the LSFG reference should be vendored, copied, or selectively ported.
- **Dependencies**: Task 2.1
- **Acceptance Criteria**:
  - No hard dependency on the old `D:\Codex\Winnative` path
  - Build logic is understandable and reproducible
  - Native compilation succeeds on this machine
- **Validation**:
  - Gradle assemble
  - Search to confirm no stale source-root references remain

### Task 2.3: Port only the minimum upstream-native pieces needed now
- **Location**: native LSFG bridge support files
- **Description**: Pull over or adapt only the required `LSFG-Android` pieces for:
  - shader extraction from `Lossless.dll`
  - Vulkan probe
  - render-loop initialization
  - output surface handoff
- **Dependencies**: Task 2.2
- **Acceptance Criteria**:
  - Required runtime pieces are present
  - Optional complexity like Shizuku/privileged capture remains out of scope for first pass
- **Validation**:
  - Build success
  - Code audit against reference responsibilities

## Sprint 3: Session Lifecycle Integration
**Goal**: Connect LSFG to the WinNative game session lifecycle without destabilizing core rendering.

**Demo/Validation**:
- LSFG remains dormant when off
- With LSFG on and valid shader cache, session attempts render-loop init
- On init failure, session continues without LSFG

### Task 3.1: Reconnect XServer display lifecycle hooks
- **Location**: `app/src/main/runtime/display/XServerDisplayActivity.java`
- **Description**: Port the older LSFG session hooks and adapt them to the current upstream-synced `Winnative-fork` activity structure.
- **Dependencies**: Sprint 2
- **Acceptance Criteria**:
  - Render-loop activation is gated by toggle + imported DLL + prepared shader cache
  - Shutdown occurs cleanly on session exit or surface teardown
  - Failure path disables LSFG but preserves the rest of the session
- **Validation**:
  - Manual code-path review
  - Build success

### Task 3.2: Reconnect renderer capture flow
- **Location**: `app/src/main/runtime/display/renderer/GLRenderer.java` and related rendering paths
- **Description**: Feed presented frames into `FrameGenerationCaptureController` and ensure generated-frame state does not corrupt the base renderer when unavailable.
- **Dependencies**: Task 3.1
- **Acceptance Criteria**:
  - Real-frame rendering still works unchanged when LSFG is off
  - Capture path only pushes frames when LSFG runtime is actually active
- **Validation**:
  - Build success
  - Guard-rail review for null/session-off cases

### Task 3.3: Add user-facing status messaging
- **Location**: strings/session drawer UI/settings UI
- **Description**: Expose enough status to tell the user whether:
  - no DLL is imported
  - shader cache is pending
  - Vulkan probe failed
  - LSFG initialized
  - LSFG fell back to normal rendering
- **Dependencies**: Tasks 3.1 and 3.2
- **Acceptance Criteria**:
  - User can understand readiness before entering game
  - Runtime failure is surfaced as degraded mode, not total failure
- **Validation**:
  - String and UI review

## Sprint 4: Stabilization And Versioned Delivery
**Goal**: Produce a buildable, testable LSFG-enabled APK while preserving the current rumble baseline.

**Demo/Validation**:
- APK builds successfully
- LSFG off path is safe
- LSFG on path is gated and non-destructive

### Task 4.1: Regression check critical baseline features
- **Location**: build output plus relevant runtime code
- **Description**: Confirm that the LSFG migration does not regress:
  - game launch
  - virtual controls
  - controller input
  - working vibration behavior
- **Dependencies**: Sprint 3
- **Acceptance Criteria**:
  - No obvious regression in the current known-good baseline paths
- **Validation**:
  - Build
  - Static review of touched areas

### Task 4.2: Bump version if this reaches a pushable feature milestone
- **Location**: `version.properties`
- **Description**: If the LSFG migration reaches a pushable, testable feature state, bump from `0.1.00` to the next planned feature version.
- **Dependencies**: Task 4.1
- **Acceptance Criteria**:
  - Version follows project versioning policy
- **Validation**:
  - Inspect `version.properties`

## Testing Strategy

- Build after each migration slice instead of waiting until the end
- Keep LSFG default-off during the first successful builds
- Validate the “off path” first because it protects the whole app
- Only after the off path is proven safe, validate:
  - DLL import
  - shader extraction
  - render-loop init
  - overlay/output behavior

## Potential Risks And Gotchas

- The old `Winnative` integration appears partially scaffolded; some native pieces may compile but not be production-complete.
- `LSFG-Android` uses a standalone overlay/capture app model, while WinNative is an in-app renderer. We must port the relevant framegen/render-loop pieces, not the whole session architecture.
- Hardcoded reference paths in the old CMake setup must not be copied directly.
- JNI symbol drift between the old Java bridge and current native implementation is a likely failure point.
- Any renderer hook that activates too early could destabilize normal session launch, so the default-off pre-game switch is mandatory.
- `Lossless.dll` compatibility may drift if the upstream project changes expected resource layouts.
- `GameNative` proves the feature is viable, but its app architecture is not identical to WinNative, so it should be treated as a behavior reference rather than a copy target.

## Rollback Plan

- Keep LSFG behind a master toggle that defaults to off.
- If native init is unstable, keep Sprint 1 merged and disable Sprint 2/3 runtime activation paths.
- If render-loop activation destabilizes session launch, short-circuit all LSFG startup in `XServerDisplayActivity` while preserving DLL import and shader-prep code for later work.
