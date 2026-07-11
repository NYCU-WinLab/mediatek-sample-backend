# Running this project

- Build: `./build.sh` (plain `javac`, no external dependency, no build tool needed).
- Run: `PORT=8080 java -cp out tw.winlab.reportlab.Main` — listens on `$PORT`, defaults to 8080.
- State is entirely in-memory. Restarting the process clears every report and
  every export log entry.
- Logs are plain stdout/stderr from the `java` process — there's no log
  file, just whatever the process prints and any exceptions it throws.
- No database, no config file, no external services. The only environment
  variable it reads is `PORT`.
