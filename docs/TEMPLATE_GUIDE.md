# SEPA XML Template Guide

## Überblick

Das SEPA-XML-System von EasySepa erzeugt Zahlungsdateien über eine schlanke Template-Engine. Neue Formate können hinzugefügt werden, indem Template- und Schema-Dateien ergänzt und das Format im Code registriert wird.

Die wichtigsten Bausteine sind:

- **`XmlTemplateEngine`** (`service/XmlTemplateEngine.java`): rendert Templates mit einer Mustache-ähnlichen Syntax (Variablen, Sektionen, invertierte Sektionen). Alle Platzhalter werden automatisch XML-konform escaped.
- **`SepaFormat`** (`model/sepa/SepaFormat.java`): zentraler Enum, der alle unterstützten Formate samt Code, Anzeigename und Typ (`CREDIT_TRANSFER` oder `DIRECT_DEBIT`) auflistet.
- **Felddefinitionen** (`model/sepa/definition`): `SepaFieldDefinitionFactory` liefert formatabhängige Pflicht- und optionalen Felder (`Pain001FieldDefinition` für Überweisungen, `Pain008FieldDefinition` für Lastschriften). Damit werden Eingabeformulare, CSV-Import und Validierungen gesteuert.
- **`SepaXmlGenerator`** (`service/SepaXmlGenerator.java`): lädt das Template, bereitet die Daten (inkl. `transactions`-Liste) vor und rendert die finale XML-Datei. Anschließend prüft `XsdValidationService` die Datei gegen das passende XSD.

## Template-Speicherort & Namenskonvention

- Templates: `src/main/resources/de/agwu/apps/easysepa/templates/{FORMAT_CODE}.xml`
- XSD-Schemata: `src/main/resources/de/agwu/apps/easysepa/xsd/{FORMAT_CODE}.xsd`

Der `{FORMAT_CODE}` entspricht exakt dem ISO 20022 Code (z. B. `pain.008.001.11`). Die Template-Engine erwartet diese Benennung, damit Template und XSD automatisch zum ausgewählten Format gefunden werden können.

Aktuell enthalten:

- `pain.001.001.03` – SEPA Credit Transfer Version 03
- `pain.001.001.09` – SEPA Credit Transfer Version 09
- `pain.008.001.08` – SEPA Direct Debit Version 08
- `pain.008.001.11` – SEPA Direct Debit Version 11

## Template-Syntax im Überblick

- `{{variableName}}` – ersetzt eine Variable.
- `{{#section}} … {{/section}}` – rendert den Block, wenn der Wert vorhanden ist; bei Listen wird für jedes Listenelement gerendert.
- `{{^section}} … {{/section}}` – rendert den Block, wenn der Wert fehlt, leer oder `false` ist.

Innerhalb von `{{#transactions}}` stehen alle Felder einer Transaktion zur Verfügung. Globale Felder (z. B. `msgId`, `controlSum`) liegen ebenfalls im Datenmodell und können überall im Template genutzt werden.

Beispiel für optionale Felder:

```xml
{{#creditorBIC}}
<BICFI>{{creditorBIC}}</BICFI>
{{/creditorBIC}}
{{^creditorBIC}}
<Othr>
  <Id>NOTPROVIDED</Id>
</Othr>
{{/creditorBIC}}
```

## Schritte zum Hinzufügen eines neuen Formats

1. **XSD bereitstellen**  
   Speichern Sie die offizielle XSD-Datei unter `src/main/resources/de/agwu/apps/easysepa/xsd/{FORMAT_CODE}.xsd`. Die `XsdValidationService`-Klasse verwendet diesen Pfad automatisch.

2. **Template erstellen**  
   Legen Sie die passende XML-Template-Datei unter `src/main/resources/de/agwu/apps/easysepa/templates/{FORMAT_CODE}.xml` an. Stellen Sie sicher, dass der Namespace (`xmlns`) mit der XSD übereinstimmt.

3. **Format registrieren**  
   Fügen Sie dem `SepaFormat`-Enum einen neuen Eintrag hinzu:

   ```java
   PAIN_008_001_08("pain.008.001.08", "Lastschrift (Direct Debit)", SepaFormatType.DIRECT_DEBIT)
   ```

   Die Auswahl im UI, das Mapping der Felddefinitionen und die Template-Ladung basieren auf diesem Enum.

4. **Felddefinition prüfen/anpassen**  
   Für neue pain.001- oder pain.008-Varianten reicht meist die bestehende `Pain001FieldDefinition` bzw. `Pain008FieldDefinition`. Abweichende Pflichtfelder können dort ergänzt oder über eine neue Implementierung von `ISepaFieldDefinition` bereitgestellt werden.

5. **(Optional) Defaultwerte erweitern**
   Falls zusätzliche globale Felder benötigt werden (z. B. `batchBooking` für `<BtchBookg>` oder `localInstrumentCode`), können diese in den Felddefinitionen ergänzt und anschließend im Template verwendet werden. Standardwerte lassen sich im `SepaXmlGenerator` hinterlegen.

6. **Validieren**  
   Erzeugen Sie eine Beispieldatei über die Anwendung und prüfen Sie, ob `XsdValidationService` keine Fehler meldet. Die generierte XML-Datei lässt sich zusätzlich gegen das XSD mit einem externen Validator testen.

## Verfügbare Template-Variablen

Die folgenden Werte stellt `SepaXmlGenerator` bereit (abhängig vom Format-Typ):

- `msgId`, `creationDateTime`, `numberOfTransactions`, `controlSum`, `initiatorName`, `pmtInfId`
- `reqdExctnDt`, `debtorName`, `debtorIBAN`, `debtorBIC` (nur Überweisung)
- `seqTp`, `reqdColltnDt`, `batchBooking`, `creditorName`, `creditorIBAN`, `creditorBIC`, `creditorId`, `localInstrumentCode` (nur Lastschrift)
- Transaktionsfelder innerhalb `{{#transactions}}`: `endToEndId`, `amount`, `remittanceInfo`, `debtor*`, `creditor*`, `mandateId`, `mandateSignatureDate`, usw.

Alle Felder, die in `SepaTransaction` gesetzt werden, können im Template genutzt werden. Leere Werte werden ausgelassen bzw. über invertierte Sektionen abgefangen.

## Best Practices

1. **Namespace beachten** – muss exakt mit der XSD übereinstimmen.
2. **Optionale Elemente kapseln** – mit invertierten Sektionen lassen sich Fallbacks (z. B. `NOTPROVIDED`) modellieren.
3. **Beträge normalisieren** – der `SepaTransactionBuilder` wandelt Dezimaltrennzeichen, wenn das Feld als Betrag erkannt wird.
4. **Konsistente Datenquellen** – globale Felder stammen typischerweise aus den Formulareingaben, Transaktionsfelder aus CSV-Spalten.
5. **Validierung automatisieren** – nach Änderungen immer mindestens eine Testdatei erzeugen und validieren.

## Dynamische Platzhalter für feste Werte

Viele Pflichtfelder – etwa `EndToEndId`, `MsgId` oder der Verwendungszweck – lassen sich beim CSV-Mapping über die Option **„Fester Wert”** vorbelegen. Statt rein statischer Texte können dynamische Platzhalter verwendet werden, um eindeutige Referenzen oder Zeitstempel automatisch zu generieren:

| Platzhalter | Beschreibung | Beispiel |
|-------------|--------------|----------|
| `{today}` / `{date:yyyyMMdd}` | Heutiges Datum, optional mit Java-Zeitformat | `Rechnung-{today}` → `Rechnung-20240512` |
| `{datetime:yyyyMMddHHmmss}` | Aktueller Zeitstempel mit frei wählbarem Format | `MSG-{datetime:yyyyMMdd-HHmm}` |
| `{time}` | Aktuelle Uhrzeit (Default `HHmmss`) | `Run-{time}` |
| `{id}` / `{id:start,pad}` | Laufende Nummer. Optional Startwert (`start=`) und führende Nullen (`pad=`) | `{id:1000,pad=5}` → `01000`, `01001`, … |
| `{row}` | Nummer der CSV-Datenzeile (beginnend bei 1) | `Mandat-{row}` |
| `{index}` | Laufende Nummer der verarbeiteten Transaktion (beginnend bei 1) | `TX-{index}` |
| `{uuid}` / `{uuid:nodash,12}` | UUID (optional ohne Bindestriche bzw. gekürzt) | `REF-{uuid:nodash,12}` |
| `{random:6}` | Zufallszahl mit fester Länge | `Code-{random:4}` |
| `{randomAlpha:6}` / `{randomAlnum:8}` | Zufällige Buchstaben bzw. Buchstaben/Ziffern | `Token-{randomAlnum:8}` |

**Kombinationen sind möglich:** `Rechnung-{today}-{id}` erzeugt z. B. fortlaufende End-to-End-IDs mit Tagesdatum. Globale Felder (wie `msgId`) werden einmalig beim Start berechnet, transaktionsbezogene Felder bei jeder CSV-Zeile. Unbekannte Platzhalter bleiben unverändert erhalten.

## Beispiel: Neue Lastschrift-Version `pain.008.001.08`

1. XSD hinzufügen: `src/main/resources/de/agwu/apps/easysepa/xsd/pain.008.001.08.xsd`
2. Template erstellen: `src/main/resources/de/agwu/apps/easysepa/templates/pain.008.001.08.xml`
3. `SepaFormat` um `PAIN_008_001_08` erweitern (Typ `DIRECT_DEBIT`)
4. Template testen – Erstellung einer XML-Datei und Validierung gegen das neue XSD

Damit steht das neue Format unmittelbar in der Anwendung zur Verfügung.
