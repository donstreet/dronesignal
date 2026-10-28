# DNR Signal — clean rebuild

This directory is a new implementation, not a copy of the legacy ZIP archives.

## Delivery sequence
1. Minimal Android Kotlin/Compose application that builds with GitHub Actions.
2. Yandex-inspired home screen and radio station list.
3. Background radio playback.
4. DNR/LNR alert state, voice notifications and Telegram/backend integration with tests.
5. Announcements feed: 9:16 thumbnail, description, external link and phone action.
6. Signed release pipeline after debug APK verification.

Existing repository files, backend and production infrastructure remain untouched until the replacement is verified.

Build target: `new-app/app/build/outputs/apk/debug/app-debug.apk`.
