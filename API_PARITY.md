# SocialBoost Android API parity

All routes are on the existing SocialBoost FastAPI backend, protected by the shared bearer token over HTTPS except login, signup and health. No second account database is created.

| App surface | Existing API routes | Status |
| --- | --- | --- |
| Authentication | POST /auth/login, POST /auth/signup, GET /auth/me | Connected |
| Home | GET /analytics/summary, GET /social/accounts | Connected |
| Insights | GET /analytics/summary, GET /analytics/posts | Connected |
| Analyze | POST /videos/upload, GET /analysis/job/{id}, GET /analysis/video/{id} | Connected |
| Library | GET /videos, DELETE /videos/{id} | Connected |
| Hook Lab | POST /analysis/hook, POST /analysis/caption | Connected |
| Predictions | GET /predictions/model, GET /predictions, POST /predictions/train, POST /predictions/run | Connected |
| Growth Coach | GET /coach/status, GET /coach/winners, POST /coach/train, POST /coach/score | Connected |
| Creator voice | POST /coach/voice/train, GET /coach/voice/status, POST /coach/voice/hooks, POST /coach/voice/write | Model dependent |
| Platforms | GET /analytics/summary | Connected |
| YouTube Studio | GET /youtube/dashboard | Server credentials/upstream dependent |
| CSV Datasets | POST /datasets/import, GET /datasets/imports | Connected |
| Calendar | GET /calendar/events, POST /calendar/schedule, PUT /calendar/{id}, POST /calendar/{id}/cancel, DELETE /calendar/{id} | Sandbox only |
| Accounts | GET /social/providers, GET /social/accounts, GET /social/oauth/{platform}/start, POST /social/accounts, POST /social/accounts/{id}/sync, DELETE /social/accounts/{id} | Connected; provider auth dependent |
| Drafts | GET /drafts, POST /drafts, PUT /drafts/{id}, DELETE /drafts/{id} | Connected |
| Settings | GET /settings/preferences, PUT /settings/preferences | Connected |
| Recommendations | GET /analytics/summary, GET /predictions/model, GET /social/accounts | Real workspace readiness; not fake ML |
| Video Autopilot | No verified production route exists | Disabled |

Some website features are richer than the current Android beta UI, and complete visual parity needs the on-device QA in the master plan.

**Important distinctions:** Model-backed text generation does not create a rendered video. Calendar items are sandbox simulations, not live social-media posts. Missing provider statistics are unavailable, not zero. OAuth callback currently returns to the website; finish consent there and refresh the Android accounts screen. Uploaded videos in the existing backend need a server-side authorization fix before public customer use.
