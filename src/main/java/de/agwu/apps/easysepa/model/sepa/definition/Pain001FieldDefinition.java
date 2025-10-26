package de.agwu.apps.easysepa.model.sepa.definition;

import de.agwu.apps.easysepa.model.sepa.SepaField;
import de.agwu.apps.easysepa.model.sepa.SepaFormat;
import de.agwu.apps.easysepa.model.sepa.SepaFormatType;

import java.util.ArrayList;
import java.util.List;

/**
 * Field definitions for pain.001 (Credit Transfer / Überweisung)
 * Works for both pain.001.001.03 and pain.001.001.09
 */
public class Pain001FieldDefinition implements ISepaFieldDefinition {

    private final SepaFormat format;

    public Pain001FieldDefinition(SepaFormat format) {
        if (format.getType() != SepaFormatType.CREDIT_TRANSFER) {
            throw new IllegalArgumentException("Format must be CREDIT_TRANSFER type");
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
            "Eindeutige Identifikation dieser SEPA-Nachricht. Wird verwendet um die gesamte Datei zu identifizieren.\n\n" +
            "Format: Max. 35 Zeichen (Buchstaben, Zahlen, Bindestriche erlaubt)\n" +
            "Beispiele: MSG-2025-10-14-001, SEPA-123456, TRANSFER-20251014-1\n\n" +
            "Tipp: Wird automatisch generiert wenn leer gelassen (Zeitstempel + Zufallszahl)"));

        fields.add(new SepaField("initiatorName", "Name des Initiators", true,
            "Name des Initiators:\n" +
            "Der Name der Person oder Firma, die diese SEPA-Datei erstellt und einreicht.\n" +
            "In der Regel ist das Ihr Firmenname oder Ihr eigener Name.\n\n" +
            "Format: Max. 70 Zeichen\n" +
            "Beispiel: Musterfirma GmbH, Max Mustermann"));

        // Payment Information fields (constant for all transactions)
        fields.add(new SepaField("pmtInfId", "Zahlungsinformations-ID", true,
            "Zahlungsinformations-ID (Payment Information ID):\n" +
            "Eindeutige ID für diese Gruppe von Zahlungen. Alle Transaktionen in dieser Datei gehören zu dieser Payment Info.\n\n" +
            "Format: Max. 35 Zeichen\n" +
            "Beispiele: PMT-2025-10-14-001, BATCH-123, PAYMENT-20251014\n\n" +
            "Tipp: Wird automatisch generiert wenn leer gelassen"));

        fields.add(new SepaField("reqdExctnDt", "Gewünschtes Ausführungsdatum", true,
            "Gewünschtes Ausführungsdatum:\n" +
            "Das Datum, an dem die Bank die Überweisungen ausführen soll.\n\n" +
            "Format: YYYY-MM-DD (Jahr-Monat-Tag)\n" +
            "Beispiel: 2025-10-20\n\n" +
            "Hinweis: Das Datum sollte in der Zukunft liegen. Bei Überweisungen mindestens 1 Bankarbeitstag Vorlauf einplanen."));

        fields.add(new SepaField("debtorName", "Name des Schuldners (Sie)", true,
            "Name des Schuldners (Auftraggeber):\n" +
            "Ihr Name oder Firmenname - das Konto von dem das Geld abgebucht wird.\n" +
            "Dies ist der Auftraggeber der Überweisung.\n\n" +
            "Format: Max. 70 Zeichen\n" +
            "Beispiel: Musterfirma GmbH"));

        fields.add(new SepaField("debtorIBAN", "IBAN des Schuldners (Sie)", true,
            "IBAN des Schuldners (Ihr Konto):\n" +
            "Die IBAN Ihres Kontos, von dem die Überweisungen ausgeführt werden.\n\n" +
            "Format: DE + 20 Ziffern (für deutsche IBANs)\n" +
            "Beispiel: DE89370400440532013000\n\n" +
            "Hinweis: Ohne Leerzeichen eingeben!"));

        fields.add(new SepaField("debtorBIC", "BIC des Schuldners (Sie)", false,
            "BIC des Schuldners (Ihre Bank):\n" +
            "Der BIC (Bank Identifier Code) Ihrer Bank.\n\n" +
            "Format: 8 oder 11 Zeichen\n" +
            "Beispiel: COBADEFFXXX, BYLADEM1001\n\n" +
            "Hinweis: Optional bei SEPA-Überweisungen innerhalb des SEPA-Raums. " +
            "Die Bank kann diesen aus der IBAN ableiten."));

        return fields;
    }

    @Override
    public List<SepaField> getTransactionFields() {
        List<SepaField> fields = new ArrayList<>();

        fields.add(new SepaField("endToEndId", "End-to-End-ID", true,
            "End-to-End-ID (Ende-zu-Ende-Referenz):\n" +
            "Eindeutige Referenz für jede einzelne Transaktion. Diese ID wird durch die gesamte Zahlungskette " +
            "weitergegeben und ermöglicht die Nachverfolgung der Zahlung.\n\n" +
            "Format: Max. 35 Zeichen\n" +
            "Beispiele: \n" +
            "  - Rechnungsnummer: RE-2025-001\n" +
            "  - Kundennummer + Datum: K123456-20251014\n" +
            "  - Fortlaufende Nummer: TXN-00001\n\n" +
            "Wichtig: Sollte für jede Transaktion unterschiedlich sein, damit Sie Zahlungen eindeutig zuordnen können.\n\n" +
            "Tipp: Mit 'Fester Wert' lassen sich Platzhalter kombinieren, etwa\n" +
            "  Rechnung-{today}-{id}    → Tagesdatum + laufende Nummer\n" +
            "  Auftrag-{row}            → nutzt die CSV-Zeilennummer\n" +
            "  REF-{uuid:nodash,12}     → erzeugt 12-stellige UUID-Segmente"));

        fields.add(new SepaField("amount", "Betrag", true,
            "Betrag:\n" +
            "Der Überweisungsbetrag in Euro.\n\n" +
            "Format: Dezimalzahl mit Punkt als Trennzeichen\n" +
            "Beispiele: 123.45, 1000.00, 42.99\n\n" +
            "Hinweis: Keine Währungsangabe, kein €-Zeichen, kein Komma! Nur Zahlen und Punkt."));

        fields.add(new SepaField("creditorName", "Name des Empfängers", true,
            "Name des Empfängers (Kreditor):\n" +
            "Der Name der Person oder Firma, die das Geld erhält.\n\n" +
            "Format: Max. 70 Zeichen\n" +
            "Beispiel: Max Mustermann, Beispiel GmbH"));

        fields.add(new SepaField("creditorIBAN", "IBAN des Empfängers", true,
            "IBAN des Empfängers:\n" +
            "Die IBAN des Kontos auf das überwiesen werden soll.\n\n" +
            "Format: DE + 20 Ziffern (für deutsche IBANs), andere Länder haben andere Längen\n" +
            "Beispiel: DE89370400440532013000\n\n" +
            "Hinweis: Ohne Leerzeichen eingeben!"));

        fields.add(new SepaField("creditorBIC", "BIC des Empfängers", false,
            "BIC des Empfängers:\n" +
            "Der BIC (Bank Identifier Code) der Bank des Empfängers.\n\n" +
            "Format: 8 oder 11 Zeichen\n" +
            "Beispiel: COBADEFFXXX, BYLADEM1001\n\n" +
            "Hinweis: Optional bei SEPA-Überweisungen innerhalb des SEPA-Raums."));

        fields.add(new SepaField("remittanceInfo", "Verwendungszweck", false,
            "Verwendungszweck:\n" +
            "Beschreibung der Zahlung, die der Empfänger auf seinem Kontoauszug sieht.\n\n" +
            "Format: Max. 140 Zeichen\n" +
            "Beispiele: \n" +
            "  - Rechnung RE-2025-001\n" +
            "  - Mitgliedsbeitrag 2025\n" +
            "  - Lohn Oktober 2025\n\n" +
            "Tipp: Geben Sie hier Informationen an, mit denen der Empfänger die Zahlung zuordnen kann."));

        return fields;
    }
}
