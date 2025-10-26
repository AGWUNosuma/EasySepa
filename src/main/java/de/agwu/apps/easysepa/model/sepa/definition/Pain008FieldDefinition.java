package de.agwu.apps.easysepa.model.sepa.definition;

import java.util.ArrayList;
import de.agwu.apps.easysepa.model.sepa.SepaField;
import de.agwu.apps.easysepa.model.sepa.SepaFormat;
import de.agwu.apps.easysepa.model.sepa.SepaFormatType;
import java.util.List;

/**
 * Field definitions for pain.008 (Direct Debit / Lastschrift)
 */
public class Pain008FieldDefinition implements ISepaFieldDefinition {

    private final SepaFormat format;

    public Pain008FieldDefinition(SepaFormat format) {
        if (format.getType() != SepaFormatType.DIRECT_DEBIT) {
            throw new IllegalArgumentException("Format must be DIRECT_DEBIT type");
        }
        this.format = format;
    }

    @Override
    public SepaFormat getFormat() {
        return format;
    }

    @Override
    public List<SepaField> getGlobalFields() {
        List<SepaField> fields = new ArrayList<>();

        // Group Header fields
        fields.add(new SepaField("msgId", "Nachrichten-ID", true,
            "Nachrichten-ID (Message ID):\n" +
            "Eindeutige Identifikation dieser SEPA-Lastschrift-Datei.\n\n" +
            "Format: Max. 35 Zeichen (Buchstaben, Zahlen, Bindestriche erlaubt)\n" +
            "Beispiele: MSG-DD-2025-10-14-001, SEPA-LS-123456\n\n" +
            "Tipp: Wird automatisch generiert wenn leer gelassen (Zeitstempel + Zufallszahl)"));

        fields.add(new SepaField("initiatorName", "Name des Initiators", true,
            "Name des Initiators:\n" +
            "Der Name der Person oder Firma, die diese SEPA-Lastschrift-Datei erstellt und einreicht.\n" +
            "In der Regel ist das Ihr Firmenname.\n\n" +
            "Format: Max. 70 Zeichen\n" +
            "Beispiel: Musterfirma GmbH"));

        // Payment Information fields (constant for all transactions)
        fields.add(new SepaField("pmtInfId", "Zahlungsinformations-ID", true,
            "Zahlungsinformations-ID (Payment Information ID):\n" +
            "Eindeutige ID für diese Gruppe von Lastschriften.\n\n" +
            "Format: Max. 35 Zeichen\n" +
            "Beispiele: PMT-DD-2025-10-14-001, LS-BATCH-123\n\n" +
            "Tipp: Wird automatisch generiert wenn leer gelassen"));

        fields.add(new SepaField("batchBooking", "Sammelbuchung", false,
            "Sammelbuchung (Batch Booking):\n" +
            "Gibt an, ob die Bank die Lastschriften als Sammelbuchung (true) " +
            "oder einzeln (false) ausführen soll.\n\n" +
            "Standard: true – die meisten Banken erwarten Sammelbuchungen."));

        fields.add(new SepaField("reqdColltnDt", "Gewünschtes Einzugsdatum", true,
            "Gewünschtes Einzugsdatum:\n" +
            "Das Datum, an dem die Bank die Lastschriften einziehen soll.\n\n" +
            "Format: YYYY-MM-DD (Jahr-Monat-Tag)\n" +
            "Beispiel: 2025-10-20\n\n" +
            "Wichtig: \n" +
            "  - FRST/OOFF: Mindestens 6 Bankarbeitstage Vorlauf\n" +
            "  - RCUR/FNAL: Mindestens 3 Bankarbeitstage Vorlauf\n" +
            "  - B2B-Lastschriften: Mindestens 2 Bankarbeitstage Vorlauf"));

        fields.add(new SepaField("creditorName", "Name des Gläubigers (Sie)", true,
            "Name des Gläubigers:\n" +
            "Ihr Name oder Firmenname - Sie sind der Zahlungsempfänger.\n" +
            "Das Konto auf das das Geld eingezogen wird.\n\n" +
            "Format: Max. 70 Zeichen\n" +
            "Beispiel: Musterfirma GmbH"));

        fields.add(new SepaField("creditorIBAN", "IBAN des Gläubigers (Sie)", true,
            "IBAN des Gläubigers (Ihr Konto):\n" +
            "Die IBAN Ihres Kontos, auf das die Lastschriftbeträge eingezogen werden.\n\n" +
            "Format: DE + 20 Ziffern (für deutsche IBANs)\n" +
            "Beispiel: DE89370400440532013000\n\n" +
            "Hinweis: Ohne Leerzeichen eingeben!"));

        fields.add(new SepaField("creditorBIC", "BIC des Gläubigers (Sie)", false,
            "BIC des Gläubigers (Ihre Bank):\n" +
            "Der BIC (Bank Identifier Code) Ihrer Bank.\n\n" +
            "Format: 8 oder 11 Zeichen\n" +
            "Beispiel: COBADEFFXXX, BYLADEM1001\n\n" +
            "Hinweis: Optional bei SEPA-Lastschriften innerhalb des SEPA-Raums."));

        fields.add(new SepaField("creditorId", "Gläubiger-Identifikationsnummer", true,
            "Gläubiger-Identifikationsnummer (Creditor ID):\n" +
            "Ihre eindeutige Gläubiger-Identifikationsnummer, die Sie von der Bundesbank erhalten haben.\n\n" +
            "Format: 18 Zeichen (DE + 2 Prüfziffern + ZZZ + 10 Zeichen)\n" +
            "Beispiel: DE98ZZZ09999999999\n\n" +
            "Wichtig: Diese Nummer müssen Sie bei der Deutschen Bundesbank beantragen!\n" +
            "Website: www.bundesbank.de/de/aufgaben/unbarer-zahlungsverkehr/serviceangebot/\n" +
            "         elektronische-verfahren/glaeubiger-identifikationsnummer"));

        fields.add(new SepaField("seqTp", "Sequenz-Typ", true,
            "Sequenz-Typ (Sequence Type):\n" +
            "Gibt an, ob es sich um eine erste, wiederholte, einmalige oder letzte Lastschrift handelt.\n\n" +
            "Mögliche Werte:\n" +
            "  • FRST (First): Erste Lastschrift eines Mandats\n" +
            "    → Verwenden bei der ersten Abbuchung nach Mandatserteilung\n\n" +
            "  • RCUR (Recurring): Folge-Lastschrift\n" +
            "    → Verwenden für alle weiteren regelmäßigen Abbuchungen\n\n" +
            "  • OOFF (One-Off): Einmalige Lastschrift\n" +
            "    → Verwenden wenn nur eine einzige Abbuchung geplant ist\n\n" +
            "  • FNAL (Final): Letzte Lastschrift eines Mandats\n" +
            "    → Verwenden für die allerletzte Abbuchung (danach Mandat ungültig)\n\n" +
            "Wichtig: Die Vorlauffristen unterscheiden sich je nach Typ!"));

        fields.add(new SepaField("localInstrumentCode", "Local Instrument Code", false,
            "Local Instrument Code:\n" +
            "Kennzeichnet das Lastschriftverfahren (z. B. CORE oder B2B).\n\n" +
            "Standard: CORE – klassische SEPA-Basislastschrift.\n" +
            "Weitere Optionen: B2B (Business-to-Business), COR1 (verkürzte Vorlauffrist, sofern unterstützt).\n\n" +
            "Wenn nichts angegeben wird, nutzt EasySepa automatisch CORE."));

        return fields;
    }

    @Override
    public List<SepaField> getTransactionFields() {
        List<SepaField> fields = new ArrayList<>();

        fields.add(new SepaField("endToEndId", "End-to-End-ID", true,
            "End-to-End-ID (Ende-zu-Ende-Referenz):\n" +
            "Eindeutige Referenz für jede einzelne Lastschrift-Transaktion.\n\n" +
            "Format: Max. 35 Zeichen\n" +
            "Beispiele: \n" +
            "  - Kundennummer: K-123456\n" +
            "  - Vertragsnummer + Datum: V789-20251014\n" +
            "  - Rechnungsnummer: RE-2025-001\n\n" +
            "Wichtig: Sollte für jede Transaktion unterschiedlich sein zur eindeutigen Zuordnung.\n\n" +
            "Tipp: Über die Option 'Fester Wert' können Sie dynamische Platzhalter verwenden, z. B.\n" +
            "  Rechnung-{today}-{id}  → erzeugt laufende IDs mit Tagesdatum\n" +
            "  Mandat-{row}           → nutzt die Zeilennummer aus der CSV\n" +
            "  TX-{datetime:yyyyMMddHHmm}-{random:4} → kombiniert Zeitstempel und Zufallszahl"));

        fields.add(new SepaField("amount", "Betrag", true,
            "Betrag:\n" +
            "Der Lastschriftbetrag in Euro.\n\n" +
            "Format: Dezimalzahl mit Punkt als Trennzeichen\n" +
            "Beispiele: 123.45, 1000.00, 42.99\n\n" +
            "Hinweis: Keine Währungsangabe, kein €-Zeichen, kein Komma! Nur Zahlen und Punkt."));

        fields.add(new SepaField("mandateId", "Mandatsreferenz", true,
            "Mandatsreferenz (Mandate Reference):\n" +
            "Die eindeutige Referenznummer des SEPA-Lastschriftmandats, das der Zahlungspflichtige unterschrieben hat.\n\n" +
            "Format: Max. 35 Zeichen\n" +
            "Beispiele: MAND-K123456-001, M-20250101-789\n\n" +
            "Wichtig: \n" +
            "  - Diese Referenz vergeben SIE selbst beim Erstellen des Mandats\n" +
            "  - Muss eindeutig sein innerhalb Ihrer Gläubiger-ID\n" +
            "  - Wird auf dem Mandat-Formular eingetragen\n" +
            "  - Meist verknüpft mit Kunden-/Vertragsnummer"));

        fields.add(new SepaField("mandateSignatureDate", "Mandats-Unterschriftsdatum", true,
            "Mandats-Unterschriftsdatum:\n" +
            "Das Datum, an dem der Zahlungspflichtige das SEPA-Lastschriftmandat unterschrieben hat.\n\n" +
            "Format: YYYY-MM-DD (Jahr-Monat-Tag)\n" +
            "Beispiel: 2024-05-15\n\n" +
            "Wichtig: Dieses Datum finden Sie auf dem unterschriebenen Mandat-Formular."));

        fields.add(new SepaField("debtorName", "Name des Schuldners", true,
            "Name des Schuldners (Zahlungspflichtiger):\n" +
            "Der Name der Person oder Firma, von deren Konto abgebucht wird.\n\n" +
            "Format: Max. 70 Zeichen\n" +
            "Beispiel: Max Mustermann, Beispiel AG\n\n" +
            "Wichtig: Muss mit dem Namen auf dem Mandat und dem Kontoinhaber übereinstimmen!"));

        fields.add(new SepaField("debtorIBAN", "IBAN des Schuldners", true,
            "IBAN des Schuldners:\n" +
            "Die IBAN des Kontos, von dem die Lastschrift eingezogen werden soll.\n\n" +
            "Format: DE + 20 Ziffern (für deutsche IBANs)\n" +
            "Beispiel: DE89370400440532013000\n\n" +
            "Wichtig: \n" +
            "  - Ohne Leerzeichen eingeben!\n" +
            "  - Muss mit der IBAN auf dem Mandat übereinstimmen"));

        fields.add(new SepaField("debtorBIC", "BIC des Schuldners", false,
            "BIC des Schuldners:\n" +
            "Der BIC (Bank Identifier Code) der Bank des Zahlungspflichtigen.\n\n" +
            "Format: 8 oder 11 Zeichen\n" +
            "Beispiel: COBADEFFXXX, BYLADEM1001\n\n" +
            "Hinweis: Optional bei SEPA-Lastschriften innerhalb des SEPA-Raums."));

        fields.add(new SepaField("remittanceInfo", "Verwendungszweck", false,
            "Verwendungszweck:\n" +
            "Beschreibung der Lastschrift, die auf dem Kontoauszug des Zahlungspflichtigen erscheint.\n\n" +
            "Format: Max. 140 Zeichen\n" +
            "Beispiele: \n" +
            "  - Mitgliedsbeitrag 2025\n" +
            "  - Rechnung RE-2025-001\n" +
            "  - Abo-Gebühr Oktober 2025\n\n" +
            "Tipp: Geben Sie Informationen an, mit denen der Kunde die Abbuchung nachvollziehen kann."));

        return fields;
    }
}
