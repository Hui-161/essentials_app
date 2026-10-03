# Sicherheitsprüfung des Essentials-Forks

Stand: 2026-10-03 (Prüfung 2026-10-02), Basis `sameerasw/essentials` main `8294f0e` (Version 18.5-beta.1).
Methode: statische Codeprüfung (Manifest, ~630 Kotlin-Dateien, Build, CI). Kein Gerätetest,
keine dynamische Analyse. Die Ergebnisse sind Vorschläge und müssen fachlich geprüft werden.
Die Korrekturen vor dem produktiven Einsatz auf einem Testgerät prüfen.

## Ausgangslage

Die App bündelt sehr weitreichende Rechte: Bedienungshilfen, Benachrichtigungszugriff, eigene
Tastatur, `WRITE_SECURE_SETTINGS`, Shell-Zugriff über Shizuku oder Root, Geräteadministrator,
Standort im Hintergrund, Anrufliste, Kontakte, Zugriff auf alle Dateien und
`REQUEST_INSTALL_PACKAGES`. Jede Lücke, über die eine andere App die Essentials-Funktionen
mitbenutzen kann, wiegt deshalb schwer: Das Risiko ist die Rechteausweitung über Essentials
(„Confused Deputy“).

Positiv: Die Bedienungshilfe liest keine Bildschirminhalte anderer Apps. Der
Benachrichtigungszugriff speichert keine Texte. Biometrie nutzt `BIOMETRIC_STRONG`. Die
PendingIntents sind unveränderlich, und der Gradle-Wrapper ist identisch mit dem offiziellen
Gradle 9.5.0.

## Befunde und Status

| # | Schwere | Befund | Status |
|---|---|---|---|
| 1 | kritisch | Intern genutzter Receiver (`AppFlowHandler`) exportiert: Jede App konnte beliebige Apps einfrieren lassen, und der Paketname floss ungeprüft in einen Shell-Befehl (Shizuku/Root). | behoben |
| 2 | hoch | Shell-Befehle als String ohne Quoting (`sh -c`, `su`): Paketnamen und Settings-Werte aus importierten Automationen/Einstellungen konnten Befehle einschleusen. | behoben (zentrale Prüfung und Quoting) |
| 3 | hoch | `EXTERNAL_CONTROL` (Schutzstufe „dangerous“, ein Dialog genügt) gab alle Einstellungen frei: GitHub- und Shizuku-Tokens lesen, App-Listen und Automations-JSON schreiben. | behoben (nur Boolean-Schalter, Schutzschalter gesperrt) |
| 4 | hoch | App Lock und Conscious Gate per Broadcast umgehbar (`APP_AUTHENTICATED` von jeder App). | behoben |
| 5 | hoch | Sentry standardmäßig aktiv, sendet Absturzberichte, Sitzungen und Gerätedaten an das Sentry-Projekt des Original-Autors, schon vor dem Onboarding. | behoben (entfernt) |
| 6 | hoch | Updater lädt APKs von einer vom Original-Server gelieferten URL ohne Hash- oder Signaturprüfung; Abhängigkeit über JitPack (nicht reproduzierbar). | behoben (nur eigene Releases, Signaturprüfung, ohne JitPack) |
| 7 | hoch | Tastatur schrieb jeden Eintrag der Zwischenablage ins Logcat (Passwörter, Einmalcodes). | behoben |
| 8 | mittel | Remap-, Taschenlampen-, AOD- und Glance-Aktionen von fremden Apps auslösbar (exportierter Receiver der Bedienungshilfe). | behoben |
| 9 | mittel | Tastatur lernt Wörter auch in Passwortfeldern und ignoriert `IME_FLAG_NO_PERSONALIZED_LEARNING`; Zwischenablage auf dem Sperrbildschirm sichtbar; Undo über App-Grenzen. | behoben |
| 10 | mittel | Exportierte interne Activities (Shut-Up-Shortcut deaktiviert ADB und Bedienungshilfen, Standort-Alarm im Vollbild, Debugging-/DNS-Dialoge). | behoben (nicht mehr exportiert) |
| 11 | mittel | WebView (`BubbleWebActivity`): `intent:`-Links ohne `selector = null` und ohne Nutzergeste, Mixed Content erlaubt. | behoben |
| 12 | mittel | Sensible Logausgaben (Rufnummern, Kontaktnamen, Benachrichtigungstexte, Koordinaten, SSIDs, URLs, API-Antworten); kein Entfernen der Logs im Release-Build. | behoben (Inhalte entfernt, R8 entfernt `Log.v/d/i`) |
| 13 | mittel | Backup ohne Regeln: gelernte Wörter und Absturzlogs gingen ins Cloud-Backup und in den Gerätetransfer. | behoben |
| 14 | niedrig | Root-Prüfung und Root-Befehle ohne Timeout (blockieren bei einer Root-Manager-Abfrage). | behoben |
| 15 | niedrig | `CallReceiver` nahm gefälschte Anruf-Broadcasts an (falscher Anruf in Island und auf der Uhr). | behoben |
| 16 | niedrig | Standort-Alarm-Liste der Schnittstelle gab Koordinaten gespeicherter Orte heraus. | behoben |
| 17 | mittel | Tokens (GitHub, Shizuku, Unsplash) im Klartext in `essentials_prefs`, damit auch im Cloud-Backup (dort Ende-zu-Ende-verschlüsselt ab Android 9 mit Bildschirmsperre). | behoben: eigene Datei `essentials_secrets` (Migration beim Start), von Backup und Gerätetransfer ausgeschlossen, nie exportiert, für `EXTERNAL_CONTROL` nicht erreichbar |
| 18 | hoch | Import von Einstellungen ohne Biometrie und Allowlist: Eine fremde Datei konnte beliebige Prefs-Dateien schreiben, App Lock abschalten, Automationen einspielen und über `shut_up_original_settings` beim nächsten Wiederherstellen eine fremde Bedienungshilfe in `Settings.Secure` eintragen; falsche Werttypen führten zu Abstürzen beim Lesen. | behoben: Biometrie/Gerätesperre vor Import und Export, nur die fünf exportierten Dateien, Geräte-Zustand und Zugangsdaten nie importiert, Typprüfung, Größenlimit 5 MB, Vorschau mit geänderten Sicherheitseinstellungen und Automationen mit Systemaktionen; beim Ersetzen bleiben nicht enthaltene Sicherheitseinstellungen erhalten |
| 19 | mittel | Link-Picker ruft jeden Link vorab ab, auch wenn die Vorschau aus ist (User-Agent `facebookexternalhit`); Favicons über Google. | offen (Online-Inhalte bewusst unverändert) |
| 20 | mittel | Online-Inhalte von Servern des Original-Autors: Hilfe-Videos, Projekt-Infos, Tages-Hintergrund, Übersetzungsmodus (GitHub-Login mit dessen OAuth-App, Kommentare in dessen Discussion). | offen (bewusst unverändert) |
| 21 | niedrig | URL-Kürzer-Kachel schickt die Zwischenablage ohne Rückfrage an `btl.dpdns.org`. | offen |
| 22 | niedrig | `ActionShortcutActivity` (Launcher-Eintrag), `ShortcutHandlerActivity` und `QSPreferencesActivity` bleiben exportiert (vom System bzw. Launcher benötigt): Fremde Apps können die konfigurierte Aktion auslösen, eingefrorene Apps starten oder die Lockdown-Kachel auslösen. | offen |
| 23 | niedrig | `file_paths.xml` gibt ganze Speicherbereiche frei (`external-path "."`); Provider nicht exportiert. | offen |
| 24 | niedrig | Exportierter Manifest-Receiver `moe.shizuku.api.SystemServiceHelper$Receiver` ohne vorhandene Klasse: Ein expliziter Broadcast ließ die App abstürzen. | behoben (entfernt) |
| 25 | info | CI des Originals: `notify.yml` (Telegram, `pull_request_target`) und `apply-translations.yml` (Push und PR aus Discussion-Kommentaren) sind auf die Infrastruktur des Original-Autors zugeschnitten. | im Fork entfernt |

## Nicht verändert, aber zu beachten

- Shizuku bzw. Root gibt Essentials Shell-Rechte (uid 2000 bzw. 0). Jede neue Funktion, die
  Werte in Shell-Befehle einsetzt, muss `ShellUtils.quote` bzw. `ShellUtils.isValidPackageName`
  verwenden.
- „Alle Berechtigungen erteilen“ (`PermissionGrantUtil`) vergibt sämtliche Rechte auf einmal.
  Datensparsamer ist es, nur die Rechte der genutzten Funktionen zu erteilen.
- Ob die App im Firmenkontext mit diesen Rechten genutzt werden darf (Bedienungshilfe,
  Benachrichtigungen, Standort), bitte vor einem Einsatz mit der IT-Abteilung klären.
