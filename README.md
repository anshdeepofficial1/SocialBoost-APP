# SocialBoost — Android

Native Kotlin + Jetpack Compose app for the existing [SocialBoost](https://github.com/anshdeepofficial1/SocialBoost) Creator Studio.

**Shared backend:** https://socialboost-yzx2.onrender.com (existing FastAPI API). **Design:** website’s warm duotone (#EFEEEA, #0C0C0A, #F0501F). No mock analytics, paid API keys, device-side video rendering or fake publishing success.

This repository is an Android client, not a replacement for the web/backend. Existing analysis, reports, hooks, predictions, connected accounts and scheduling are used through authenticated API routes. Features that the existing backend has not yet implemented (video generation and live publishing) must remain visibly unavailable until those endpoints have been built and tested.

## Build and testing

Open the repository in Android Studio (JDK 17), sync Gradle, install Android SDK 35 and run the `app` configuration. GitHub Actions will assemble the debug APK and publish it as a CI artifact. Release builds require your own signing configuration; do not publish debug APKs to stores.

See [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) for source-backed implementation state and release gates.
