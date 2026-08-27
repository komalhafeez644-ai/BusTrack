# Help & Support Chatbot — Setup

## API/service used
**OpenAI Chat Completions API** (`gpt-4o-mini`), called as a plain HTTPS REST request
using OkHttp — which is already part of this project (pulled in transitively by the
`retrofit2` dependency already in `app/build.gradle.kts`). No new Gradle dependency was
added, and no Kotlin/Gradle/AGP version was changed.

Why this one:
- Plain REST call, one JSON request/response — no client SDK, no minSdk bump, no
  compatibility risk with the project's Kotlin 2.0.21 / AGP 8.7.2 setup.
- The endpoint is OpenAI-compatible, so if you'd rather use a different provider
  (Groq, OpenRouter, Together.ai, your own hosted model, etc.), you only need to change
  `CHATBOT_API_URL` and `MODEL` in
  `app/src/main/java/com/example/bustrack_app/data/ChatbotRepository.kt` — the
  request/response shape is the same for all of them.
- Not Gemini, per your requirement.

## Where to put your API key
1. Open (or create) `local.properties` in the **project root** (same folder as
   `settings.gradle.kts`). This file is already git-ignored — it's the same place the
   project already stores `MAPBOX_ACCESS_TOKEN`.
2. Add one line:
   ```
   CHATBOT_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
   ```
3. Rebuild. The key is read at build time into `BuildConfig.CHATBOT_API_KEY` and is
   **never hardcoded in source or committed to git**.

If you don't set a key, the chatbot screen still opens normally and shows a friendly
"Chatbot isn't configured yet. Please contact the app administrator." message instead of
crashing.

## What was changed
- `app/build.gradle.kts` — added `CHATBOT_API_KEY` as a `buildConfigField`, read from
  `local.properties` (same pattern already used for Mapbox).
- `ChatbotRepository.kt` (new) — the actual API call, with a system prompt scoped to
  bus tracking / attendance / routes / stops / parent tracking / driver usage / app usage.
- `ChatbotActivity.kt` (new) + `activity_chatbot.xml` (new) — the chat UI itself
  (message bubbles, loading indicator, error handling).
- Existing FAQ screens (Admin, Parent, Driver — Principal reuses Admin's) each got one
  new "Chat with us" button wired to open this screen. Nothing else in those screens was
  changed.
