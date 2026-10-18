# Änderungen im Fork

Änderungen dieses Forks (`Hui-161/essentials_app`) gegenüber dem Original. Der oberste Abschnitt
wird beim Release zu den Release-Notizen und erscheint auch in der App beim Update. Vor jedem
Release oben einen neuen Abschnitt anlegen. Die Versionsnummer mit Build-Nummer vergibt der
Release-Workflow (`v<Version>-fork.<Anzahl Commits>`).

## 18.5-beta.1-fork – erste eigene Version (Oktober 2026)

Basis: Essentials 18.5-beta.1 (Original-Stand `8294f0e`). Neuere Versionen des Originals
(19.0, 20.0) sind noch nicht enthalten.

### Datenschutz und Sicherheit

- Keine Telemetrie mehr: Sentry ist vollständig entfernt.
- Sicherheitsprüfung mit 25 Befunden: 22 behoben, einer weitgehend behoben, 2 bewusst offen
  (Online-Inhalte des Originals). Unter anderem können andere Apps keine internen Aktionen mehr
  auslösen (Apps einfrieren, App-Sperre umgehen, Taschenlampe, Remaps). Werte in Shell-Befehlen
  werden geprüft und maskiert. Die externe Steuerung ist auf einfache Schalter begrenzt. Details
  stehen in [docs/SECURITY_REVIEW.md](docs/SECURITY_REVIEW.md).
- Export und Import von Einstellungen verlangen Fingerabdruck oder Gerätesperre. Der Import zeigt
  vorher geänderte Sicherheitseinstellungen und Automationen mit Systemaktionen. Zugangsdaten
  werden nie exportiert und sind vom Backup ausgeschlossen.
- Die Tastatur protokolliert keine Zwischenablage mehr und lernt keine Wörter in
  Passwortfeldern. Rufnummern, Kontaktnamen, Benachrichtigungstexte, Koordinaten und ähnliche
  Inhalte stehen nicht mehr in Logs. Gelernte Wörter und Absturzlogs sind vom Backup
  ausgeschlossen.
- Der URL-Kürzer sendet erst nach Bestätigung. Verknüpfungen auf dem Startbildschirm sind
  weitgehend gegen Missbrauch durch andere Apps geschützt.

### Updates

- Updates kommen nur aus den Releases dieses Repositorys und werden nur installiert, wenn sie
  mit demselben Zertifikat signiert sind wie die installierte App.

### Taschenlampe

- **Taschenerkennung schaltet auf dem Tisch nicht mehr ab.** Bisher reichte ein verdeckter
  Näherungssensor, also auch ein Handy, das mit dem Display nach unten auf dem Tisch liegt. Jetzt
  wird zusätzlich die Lage geprüft: Liegt das Handy flach (Schwerkraft senkrecht zum Display über
  8 m/s², also weniger als etwa 35° geneigt, Display oben oder unten), bleibt die Lampe an. Der
  Lagesensor (Schwerkraft, sonst Beschleunigung) ist nur aktiv, solange die Lampe leuchtet und die
  Taschenerkennung an ist. Neuer Schalter „Nicht abschalten, wenn das Handy flach liegt“
  (Standard: an).
- **Neue Einstellung „Start-Helligkeit“:** Standard (Vorgabe des Systems, oft 80 %), 100 % oder
  zuletzt verwendet. Sie gilt beim Einschalten über Essentials (Kachel, Tasten, Gesten,
  Automationen) und mit „Globale Steuerungen“ auch beim Einschalten über die System-Kachel. Ohne
  Auswahl bleibt das bisherige Verhalten. Bisher wurde beim Einschalten ohne Überblenden immer die
  System-Standardstufe genutzt, auch wenn die zuletzt verwendete Helligkeit eingestellt war.
- **Plus und Minus in der Benachrichtigung reagieren wieder.** Die Tasten gehen direkt an die
  laufende Bedienungshilfe. Vorher liefen sie über `startService`, und schlug das fehl, wurde der
  Tastendruck verworfen. Die Schritte (ein Fünftel der Maximalstufe) gehen von der tatsächlichen
  Stufe aus, auch bei schnellem Tippen. Die Prozentanzeige ist gerundet und wird sofort
  aktualisiert. Bei Helligkeitsänderungen an anderer Stelle (Island, Helligkeitsdialog, System)
  zieht die Anzeige nach. Plus und Minus erscheinen nur, wenn das Gerät die Helligkeit regeln kann.
- Die Live-Benachrichtigung erscheint jetzt auch beim Einschalten ohne Überblenden. Ihre Texte
  sind übersetzt.
- Unverändert: Der Überhitzungsschutz senkt die Helligkeit erst nach 2 Minuten auf 75 % der
  Maximalstufe (abgerundet, bei 5 Stufen also auf Stufe 3).
- Aus dem Original übernommen: Absturzschutz in den Taschenlampen-Callbacks der Island
  (`edf0565d`).

### Bekannte Einschränkungen

- AirSync-Brücke und Wear-OS-App des Original-Autors funktionieren mit diesem Build nicht.
- Online-Inhalte (Hilfe-Videos, Projekt-Infos, Tages-Hintergrund, Übersetzungsmodus,
  Link-Vorschau) kommen weiterhin von den Servern des Original-Autors bzw. von GitHub.
