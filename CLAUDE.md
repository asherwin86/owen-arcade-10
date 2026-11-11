# Arcade 10

An Android arcade app (Kotlin, Jetpack Compose) plus a small Node/Express server hosted on Render.
The server serves a landing page with a download button for the APK, a few playable browser mini-games,
and a leaderboard API.

## Layout

- `app/`: the Android app (package `com.example`, applicationId `com.aistudio.arcadeten.gmxp`).
  - `ui/games/`: one file per game (Snake, 2048, Minesweeper, Brick Breaker, Word Guess, Tic-Tac-Toe,
    Flappy-style Tap Flight, Memory Match, Connect 4, Whack-a-Mole, Owen's Tag).
  - `data/`: Room database plus the Firestore global leaderboard.
  - `update/`, `ui/update/`: in-app update manager and dialog.
  - `audio/`: chiptune music and AI-generated music.
- `server.js`: Express server. Serves `public/`, the APK at `/download/arcade-10.apk`,
  `/api/app-info`, `/api` (endpoint list) and `/api/leaderboard/:gameId` (GET and POST).
- `public/index.html`: landing page. `public/games/*.js` are the browser mini-games; each one pushes
  `{ id, name, mount }` onto `window.ArcadeGames`, and `mount(el)` returns a destroy function.
- `releases/arcade-10.apk`: the built APK that the site serves. It is committed to the repo.
- `render.yaml`: Render web service `arcade-10-backend` (Node, free plan).

## Commands

Server (needs Node):

```bash
npm install
npm start
```

It listens on `PORT`, default 3000. If another program already holds port 3000 on your machine, set `PORT` to something else.

Android (AGP 9.1.1, Kotlin 2.2.10, Gradle 9.3.1, Java 11 target):

```bash
gradle :app:assembleDebug
```

`gradlew` and the wrapper jar are not committed, only `gradle/wrapper/gradle-wrapper.properties`. Use a local
Gradle install or Android Studio, or add the wrapper with `gradle wrapper`. The build needs the Android SDK.

## Releasing a new APK

1. Build a release APK. Signing reads `KEYSTORE_PATH` (default `my-upload-key.jks` in the repo root),
   `STORE_PASSWORD` and `KEY_PASSWORD` from the environment.
2. Replace `releases/arcade-10.apk`, commit and push. Render redeploys and the download updates.

## Gotchas

- The server's leaderboard is in memory only and resets on every restart or Render sleep. The Android app uses
  Firestore for its real global leaderboard.
- Render's free plan sleeps when idle, so the first request can take about 30 seconds.
- Owen's Tag is a separate site (`https://owen-tag-game.onrender.com/`). The web page links out to it and the
  Android app loads it in a WebView.
- Secrets: `.env`, `debug.keystore`, `local.properties` and keystores are gitignored. `.env.example` holds
  placeholders only (`GEMINI_API_KEY`, `FIREBASE_*`). Never commit real keys or tokens.
- Git on Windows prints "LF will be replaced by CRLF" warnings. They are harmless.
