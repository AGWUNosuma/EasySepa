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

Beispiele:
- `pain.001.001.03.xml` - Credit Transfer ISO 20022 Version 03
- `pain.001.001.09.xml` - Credit Transfer ISO 20022 Version 09
- `pain.008.001.08.xml` - Direct Debit ISO 20022 Version 08
- `pain.008.001.11.xml` - Direct Debit ISO 20022 Version 11

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

Registrieren Sie das Format in der `SepaFormat`-Enum und hinterlegen Sie die Felddefinition in der `SepaFieldDefinitionFactory`:

```java
public enum SepaFormat {
    PAIN_001_001_03("pain.001.001.03", "Überweisung (Credit Transfer)", SepaFormatType.CREDIT_TRANSFER),
    PAIN_001_001_09("pain.001.001.09", "Überweisung (Credit Transfer)", SepaFormatType.CREDIT_TRANSFER),
    PAIN_008_001_08("pain.008.001.08", "Lastschrift (Direct Debit)", SepaFormatType.DIRECT_DEBIT),
    PAIN_008_001_11("pain.008.001.11", "Lastschrift (Direct Debit)", SepaFormatType.DIRECT_DEBIT),
    PAIN_001_001_XX("pain.001.001.XX", "Überweisung (Credit Transfer)", SepaFormatType.CREDIT_TRANSFER); // Neues Format
}
```

```java
public final class SepaFieldDefinitionFactory {
    public static ISepaFieldDefinition create(SepaFormat format) {
        return switch (format) {
            case PAIN_001_001_03 -> new Pain001FieldDefinition(format);
            case PAIN_001_001_09 -> new Pain001FieldDefinition(format);
            case PAIN_008_001_08 -> new Pain008FieldDefinition(format);
            case PAIN_008_001_11 -> new Pain008FieldDefinition(format);
            case PAIN_001_001_XX -> new Pain001FieldDefinition(format); // Neues Format hinterlegen
        };
    }
}
```

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
   Falls zusätzliche globale Felder benötigt werden (z. B. `localInstrumentCode`), können diese in den Felddefinitionen ergänzt und anschließend im Template verwendet werden.

6. **Validieren**  
   Erzeugen Sie eine Beispieldatei über die Anwendung und prüfen Sie, ob `XsdValidationService` keine Fehler meldet. Die generierte XML-Datei lässt sich zusätzlich gegen das XSD mit einem externen Validator testen.

## Verfügbare Template-Variablen

### Globale Variablen (automatisch gefüllt):
- `msgId` - Message ID
- `creationDateTime` - Zeitstempel (ISO 8601)
- `numberOfTransactions` - Anzahl der Transaktionen
- `controlSum` - Summe aller Beträge
- `initiatorName` - Name des Initiators
- `pmtInfId` - Payment Information ID

### Credit Transfer spezifisch:
- `reqdExctnDt` - Ausführungsdatum
- `debtorName` - Schuldnername
- `debtorIBAN` - Schuldner-IBAN
- `debtorBIC` - Schuldner-BIC (optional)

### Direct Debit spezifisch:
- `seqTp` - Sequenztyp (FRST, RCUR, OOFF, FNAL)
- `reqdColltnDt` - Einzugsdatum
- `creditorName` - Gläubigername
- `creditorIBAN` - Gläubiger-IBAN
- `creditorBIC` - Gläubiger-BIC (optional)
- `creditorId` - Gläubiger-ID
- `batchBooking` - Gibt an, ob Buchungen gebündelt verarbeitet werden (true/false)
- `localInstrument` - Lokaler Instrumenten-Code (z. B. `B2B` oder `CORE`)

### Transaktionsfelder (innerhalb {{#transactions}}):
- `endToEndId` - End-to-End Referenz
- `amount` - Betrag
- `creditorName` / `debtorName` - Name
- `creditorIBAN` / `debtorIBAN` - IBAN
- `creditorBIC` / `debtorBIC` - BIC (optional)
- `remittanceInfo` - Verwendungszweck (optional)
- `mandateId` - Mandatsreferenz (nur Direct Debit)
- `mandateSignatureDate` - Mandatsunterschriftsdatum (nur Direct Debit)
- `endToEndId` kann mithilfe von Platzhaltern dynamisch erzeugt werden (siehe Abschnitt "Dynamische Standardwerte")

## Dynamische Standardwerte

Feste Werte, die in der UI über "Fester Wert" hinterlegt werden, können Platzhalter enthalten, um pro Transaktion eindeutige Werte zu erzeugen. Das ist besonders hilfreich für Felder wie `EndToEndId`.

Verfügbare Platzhalter:

- `{id}` – Sequenzieller Zähler, beginnend bei 1
- `{today}` – Heutiges Datum im Format `yyyy-MM-dd`
- `{timestamp}` – Aktuelle Uhrzeit im Format `yyyyMMddHHmmss`
- `{uuid}` – Zufälliger UUID-String ohne Bindestriche
- `{rand:N}` – Zufällige alphanumerische Zeichenkette mit Länge `N`

Platzhalter können kombiniert werden, z. B. `Rechnung-{today}-{id}`. Alles außerhalb der geschweiften Klammern wird unverändert übernommen. Die Auflösung erfolgt beim Einlesen jeder Transaktion, sodass jede Zeile einen eindeutigen Wert erhält.

## XSD-Validierung

Alle generierten XML-Dateien werden automatisch gegen ihre entsprechende XSD-Schema-Datei validiert. Bei Validierungsfehlern wird eine detaillierte Fehlermeldung angezeigt.

## Best Practices

1. **Namespace beachten** – muss exakt mit der XSD übereinstimmen.
2. **Optionale Elemente kapseln** – mit invertierten Sektionen lassen sich Fallbacks (z. B. `NOTPROVIDED`) modellieren.
3. **Beträge normalisieren** – der `SepaTransactionBuilder` wandelt Dezimaltrennzeichen, wenn das Feld als Betrag erkannt wird.
4. **Konsistente Datenquellen** – globale Felder stammen typischerweise aus den Formulareingaben, Transaktionsfelder aus CSV-Spalten.
5. **Validierung automatisieren** – nach Änderungen immer mindestens eine Testdatei erzeugen und validieren.

## Beispiel: Neue Lastschrift-Version `pain.008.001.08`

1. XSD hinzufügen: `xsd/pain.001.001.12.xsd`
2. Format-Definition: `Pain001001_12.java`
3. Template: `templates/pain.001.001.12.xml`
4. `SepaFormat`-Enum und `SepaFieldDefinitionFactory` erweitern

Damit steht das neue Format unmittelbar in der Anwendung zur Verfügung.
