# Android QA and Release Matrix

## Automated

- [x] Compile native Android app on GitHub-hosted Linux with JDK 17 and SDK 35
- [x] Execute Android debug unit-test task
- [x] Publish APK as a downloadable GitHub Actions artifact
- [x] Official Gradle wrapper is checked into source
- [ ] Add real HTTP contract tests with controlled mock server responses
- [ ] Android Compose screenshot tests at three screen widths
- [ ] UI accessibility and TalkBack audit
- [ ] Signed release build and security vulnerability scans

## Manual physical-device checks (not yet completed)

1. Install debug APK and verify launch, no crash, warm duotone presentation.
2. Register throwaway account, logout and login; verify shared website workspace.
3. Upload real short 9:16 MP4 and inspect server job progress, including failure case.
4. Try unsupported file, invalid session and temporary offline network.
5. Inspect actual total views, unknown metric labels and profile sync timestamps.
6. Import a CSV containing valid, duplicate and invalid rows; inspect report.
7. Train prediction/Growth Coach using enough real posts; inspect sparse-data fallback.
8. Hook Lab: analyze and save a draft, update and delete it.
9. Sandbox schedule: create, edit, cancel and delete; confirm nothing is reported published.
10. OAuth account: complete consent in browser, return to Android, refresh and sync.
11. Verify app theme preference, small screen, tablet, landscape and large text.
12. Test second account to verify no shared media/draft/schedule leakage.
13. Verify first paid provider remains disabled and no video creation occurs on phone.
14. Record build hash, CI result, screenshots and real network logs with secrets redacted.

Server-side media generation and automatic real publishing cannot pass until those shared-backend endpoints exist and are verified. Do not check those gates as complete prematurely.
