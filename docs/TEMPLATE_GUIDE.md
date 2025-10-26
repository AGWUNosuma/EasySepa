# SEPA XML Template Guide

## Übersicht

Das SEPA XML-Generierungssystem verwendet ein Template-basiertes Verfahren, das die einfache Erweiterung um neue SEPA-Formate ermöglicht.

## Template-System

### Template-Engine

Die `XmlTemplateEngine`-Klasse implementiert eine einfache Mustache-ähnliche Template-Syntax:

- `{{variableName}}` - Einfache Variable einsetzen
- `{{#section}}...{{/section}}` - Bedingte Sektion oder Schleife
- `{{^section}}...{{/section}}` - Invertierte Sektion (nur wenn Wert leer/false)

### Template-Speicherort

Alle Templates befinden sich in:
```
src/main/resources/de/agwu/apps/easysepa/templates/
```

Dateiname-Format: `{SEPA_FORMAT_CODE}.xml`

Beispiele:
- `pain.001.001.03.xml` - Credit Transfer ISO 20022 Version 03
- `pain.001.001.09.xml` - Credit Transfer ISO 20022 Version 09
- `pain.008.001.08.xml` - Direct Debit ISO 20022 Version 08
- `pain.008.001.11.xml` - Direct Debit ISO 20022 Version 11

## Neues Format hinzufügen

### Schritt 1: XSD-Schema hinzufügen

Legen Sie die XSD-Datei ab in:
```
src/main/resources/de/agwu/apps/easysepa/xsd/{FORMAT_CODE}.xsd
```

### Schritt 2: Format-Definition erstellen

Erstellen Sie eine neue Klasse in `de.agwu.apps.easysepa.model.sepa.definition`:

```java
public class Pain001001XX implements SepaFormatDefinition {
    @Override
    public String getCode() {
        return "pain.001.001.XX";
    }

    @Override
    public String getDisplayName() {
        return "SEPA Credit Transfer v.XX";
    }

    @Override
    public SepaFormatType getType() {
        return SepaFormatType.CREDIT_TRANSFER;
    }

    @Override
    public List<SepaField> getGlobalFields() {
        // Definieren Sie globale Felder
    }

    @Override
    public List<SepaField> getTransactionFields() {
        // Definieren Sie Transaktionsfelder
    }
}
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

### Schritt 3: XML-Template erstellen

Erstellen Sie eine Template-Datei:
```
src/main/resources/de/agwu/apps/easysepa/templates/pain.001.001.XX.xml
```

Template-Struktur:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.XX">
  <CstmrCdtTrfInitn>
    <GrpHdr>
      <MsgId>{{msgId}}</MsgId>
      <CreDtTm>{{creationDateTime}}</CreDtTm>
      <NbOfTxs>{{numberOfTransactions}}</NbOfTxs>
      <CtrlSum>{{controlSum}}</CtrlSum>
      <InitgPty>
        <Nm>{{initiatorName}}</Nm>
      </InitgPty>
    </GrpHdr>
    <PmtInf>
      <!-- Globale Payment Information Felder -->
      <PmtInfId>{{pmtInfId}}</PmtInfId>
      <!-- ... -->
      
      <!-- Transaktionsschleife -->
      {{#transactions}}
      <CdtTrfTxInf>
        <PmtId>
          <EndToEndId>{{endToEndId}}</EndToEndId>
        </PmtId>
        <Amt>
          <InstdAmt Ccy="EUR">{{amount}}</InstdAmt>
        </Amt>
        <!-- Weitere Transaktionsfelder -->
      </CdtTrfTxInf>
      {{/transactions}}
    </PmtInf>
  </CstmrCdtTrfInitn>
</Document>
```

### Schritt 4: Optionale BIC-Felder

Für optionale Felder wie BIC verwenden Sie bedingte Sektionen:

```xml
{{#debtorBIC}}
<BICFI>{{debtorBIC}}</BICFI>
{{/debtorBIC}}
{{^debtorBIC}}
<Othr>
  <Id>NOTPROVIDED</Id>
</Othr>
{{/debtorBIC}}
```

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

1. **XSD zuerst**: Beginnen Sie immer mit der XSD-Schema-Datei als Referenz
2. **Template-Validierung**: Testen Sie das Template mit Beispieldaten
3. **Namespace korrekt**: Achten Sie auf den korrekten XML-Namespace in der Document-Element
4. **Pflichtfelder**: Markieren Sie Pflichtfelder in der Format-Definition als `required(true)`
5. **Optionale Felder**: Verwenden Sie bedingte Sektionen für optionale Elemente
6. **XML-Escaping**: Die Template-Engine escaped automatisch alle Variablen

## Beispiel: Neues Format pain.001.001.12

1. XSD hinzufügen: `xsd/pain.001.001.12.xsd`
2. Format-Definition: `Pain001001_12.java`
3. Template: `templates/pain.001.001.12.xml`
4. `SepaFormat`-Enum und `SepaFieldDefinitionFactory` erweitern

**Fertig!** Das neue Format ist sofort in der Anwendung verfügbar.
