# UI preview without an emulator

Renders real Compose components (the app's own theme, shapes, typography, badges, settings list, auto scroll panel,
reader bottom bar) to PNG files on the JVM, using Robolectric and Roborazzi. It does not run the whole app, so screens
that need data (library, manga details) are not covered; those are approximated with plain Material components.

Run, from the repository root (needs the Android SDK; ANDROID_HOME):

    ROBOLECTRIC_DEPS=/path/to/robolectric-deps PREVIEW_OUT=/path/to/out \
      ./gradlew :app:testDebugUnitTest --tests "*DesignPreviewTest*" -PuiPreview -Pinclude-telemetry=false

* `-PuiPreview` switches on the extra test libraries and the `app/src/uiPreview` sources. Without it, nothing changes.
* `ROBOLECTRIC_DEPS` is a folder holding `android-all-instrumented-15-robolectric-13954326-i7.jar` (Robolectric cannot
  download it behind some proxies; fetch it once from Maven Central).
* Output: `default-theme-dark.png` (default theme, Warm Charcoal), `settings.png`, `reader-overlay.png` in `PREVIEW_OUT` (default `app/build/ui-preview`).
* To add a screen, add a test in `app/src/uiPreview/java/eu/kanade/presentation/preview/DesignPreviewTest.kt` that
  calls `capture("name.png") { ... }`.
