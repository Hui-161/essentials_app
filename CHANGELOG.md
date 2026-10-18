# Änderungen im Fork

Änderungen dieses Forks (`Hui-161/essentials_app`) gegenüber dem Original. Sicherheitskorrekturen
stehen in [docs/SECURITY_REVIEW.md](docs/SECURITY_REVIEW.md), Releases und Updates in
[docs/FORK_RELEASE.md](docs/FORK_RELEASE.md).

## Unveröffentlicht

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
