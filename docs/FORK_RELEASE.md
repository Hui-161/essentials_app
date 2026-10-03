# Eigene Releases dieses Forks

Dieser Fork (`Hui-161/essentials_app`) baut und veröffentlicht Essentials selbst. Unterschiede zum
Original (`sameerasw/essentials`):

- **Keine Telemetrie:** Sentry (Absturzberichte, Sitzungen an das Sentry-Projekt des Original-Autors)
  ist vollständig entfernt. Feedback öffnet ein GitHub-Issue in diesem Repository.
- **Eigener Updater:** prüft nur die GitHub-Releases dieses Repositorys, lädt nur von
  `github.com/Hui-161/essentials_app/releases/download/` und installiert nur APKs, die mit
  demselben Zertifikat signiert sind wie die installierte App. Die JitPack-Bibliothek
  `AutoUpdater` entfällt.
- **Sicherheitskorrekturen:** siehe [SECURITY_REVIEW.md](SECURITY_REVIEW.md).
- **Eigene Pipeline:** `.github/workflows/build.yml` (Debug-APK bei jedem Push) und
  `.github/workflows/release.yml` (signiertes Release per Knopfdruck). Die Upstream-Workflows
  für Telegram-Benachrichtigungen und Community-Übersetzungen sind entfernt.

Paketname bleibt `com.sameerasw.essentials`. Versionen: `versionName` = Upstream-Version +
`-fork.<Build>`, `versionCode` = Anzahl der Commits (z. B. `18.5-beta.1-fork.7930`).

## Einmalige Einrichtung

1. **Actions aktivieren:** Repository → *Actions* → „I understand my workflows, go ahead and
   enable them“ (bei Forks standardmäßig aus).
2. **Release-Schlüssel erzeugen** (lokal, nicht in einer Cloud-Sitzung; Passwort sicher wählen
   und z. B. im Passwortmanager ablegen):

   ```bash
   keytool -genkeypair -keystore release.keystore -alias essentials \
     -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Essentials Fork"
   base64 -w0 release.keystore > release.keystore.b64
   ```

   Die Datei `release.keystore` ist der wichtigste Schlüssel des Projekts: Geht sie verloren,
   kann keine installierte App mehr aktualisiert werden. Sicher aufbewahren (offline-Kopie),
   nie committen (`*.keystore` ist gitignoriert).
3. **Dev-Schlüssel für Debug-Builds** (darf ein einfaches Passwort haben, signiert nur
   `com.sameerasw.essentials.debug`):

   ```bash
   keytool -genkeypair -keystore dev.keystore -storepass android -keypass android \
     -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Dev"
   base64 -w0 dev.keystore > dev.keystore.b64
   ```

4. **Secrets anlegen:** Repository → *Settings* → *Secrets and variables* → *Actions* →
   *New repository secret*:

   | Secret | Inhalt |
   |---|---|
   | `RELEASE_KEYSTORE_BASE64` | Inhalt von `release.keystore.b64` |
   | `RELEASE_STORE_PASSWORD` | Keystore-Passwort |
   | `RELEASE_KEY_ALIAS` | `essentials` (bzw. der gewählte Alias) |
   | `RELEASE_KEY_PASSWORD` | Schlüssel-Passwort (bei PKCS12 = Keystore-Passwort) |
   | `DEV_KEYSTORE_BASE64` | Inhalt von `dev.keystore.b64` |

   Danach die `.b64`-Dateien löschen. Zugangsdaten nie in Issues, Chats oder Commits einfügen.

## Release veröffentlichen

1. Änderungen auf `main` pushen und abwarten, dass der Workflow **Build** grün ist.
2. *Actions* → **Release** → *Run workflow* → Branch `main`.
3. Ergebnis: Tag `v<versionName>`, Release mit `essentials-<versionName>.apk`, `.sha256` und
   dem **SHA-256 des Signaturzertifikats** in den Release-Notizen.

Den Zertifikat-Fingerabdruck beim ersten Release notieren. Er muss bei allen folgenden
Releases gleich bleiben.

## Installation

- **Erstinstallation:** Die Original-App ist anders signiert. Vorher in der Original-App
  *Einstellungen → Exportieren* nutzen, dann die Original-App deinstallieren, das APK aus dem
  Release installieren, Einstellungen importieren und Berechtigungen (Bedienungshilfe,
  Shizuku, Benachrichtigungszugriff …) neu erteilen. Nur selbst exportierte Dateien importieren.
  Export und Import verlangen Fingerabdruck bzw. Gerätesperre; der Import zeigt vorher, welche
  Sicherheitseinstellungen und Automationen mit Systemaktionen er ändert. Zugangsdaten
  (GitHub-Login, Shizuku-Token) werden nicht übertragen und müssen neu eingerichtet werden.
- **Updates:** In der App über *Nach Updates suchen* oder automatisch. Installiert werden nur
  Releases dieses Repositorys mit gleichem Signaturzertifikat.
- **Debug-Builds** (Artefakt `essentials-debug-apk` unter *Actions* → Lauf → *Artifacts*) heißen
  `com.sameerasw.essentials.debug` und laufen parallel zur Release-App. Sie sind zum Testen da,
  nicht für den Alltag.

## Änderungen aus dem Original übernehmen

```bash
git remote add upstream https://github.com/sameerasw/essentials   # einmalig
git fetch upstream
git checkout main
git merge upstream/main
```

Vor dem Push den Diff prüfen, besonders:

- `app/src/main/AndroidManifest.xml`: neue Berechtigungen, neue `exported="true"`-Komponenten
- neue Netzwerkziele (`git diff ORIG_HEAD -- app/src | grep -E 'https?://'`)
- neue Shell-Aufrufe mit Variablen (`runCommand(`, `newProcess(`): Werte mit `ShellUtils.quote`
  bzw. `ShellUtils.isValidPackageName` absichern
- `registerReceiver(..., RECEIVER_EXPORTED)` für App-interne Aktionen
- `gradle/libs.versions.toml` und `settings.gradle.kts`: neue Abhängigkeiten oder Repositories
- Sentry bzw. andere Telemetrie, die wieder hinzukommt

Der Fork-Block am Ende von `app/build.gradle.kts` (Version, Signatur, `RELEASE_REPO`)
überschreibt die Upstream-Werte und muss bei Merges normalerweise nicht angepasst werden.

## Bekannte Einschränkungen

- **AirSync-Brücke** und die **Wear-OS-App** (`essentials-wear`) des Original-Autors setzen
  dessen Signatur voraus und funktionieren mit diesem Build nicht. Ist AirSync installiert und
  definiert dieselbe Berechtigung, kann Android die Installation verweigern.
- Online-Inhalte (Hilfe-Videos, Projekt-Infos, Tages-Hintergrund, Übersetzungsmodus,
  Link-Vorschau) kommen weiterhin von den Servern des Original-Autors bzw. von GitHub.
