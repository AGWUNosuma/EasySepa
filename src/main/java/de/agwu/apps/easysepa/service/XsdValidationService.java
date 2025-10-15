package de.agwu.apps.easysepa.service;

import de.agwu.apps.easysepa.model.sepa.SepaFormat;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for validating SEPA XML files against XSD schemas
 */
public class XsdValidationService {

    /**
     * Validate SEPA XML file against its XSD schema
     *
     * @param xmlFile XML file to validate
     * @param format SEPA format
     * @return Validation result with errors (empty list if valid)
     */
    public ValidationResult validateXml(File xmlFile, SepaFormat format) {
        List<String> errors = new ArrayList<>();

        try {
            // Load XSD schema from resources
            String xsdPath = "/de/agwu/apps/easysepa/xsd/" + format.getCode() + ".xsd";
            InputStream xsdStream = getClass().getResourceAsStream(xsdPath);

            if (xsdStream == null) {
                errors.add("XSD Schema nicht gefunden: " + xsdPath);
                return new ValidationResult(false, errors);
            }

            // Create schema
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(xsdStream));

            // Create validator
            Validator validator = schema.newValidator();

            // Collect validation errors
            List<String> validationErrors = new ArrayList<>();
            validator.setErrorHandler(new org.xml.sax.ErrorHandler() {
                @Override
                public void warning(org.xml.sax.SAXParseException exception) {
                    validationErrors.add("Warnung: " + exception.getMessage());
                }

                @Override
                public void error(org.xml.sax.SAXParseException exception) {
                    validationErrors.add("Fehler: " + exception.getMessage());
                }

                @Override
                public void fatalError(org.xml.sax.SAXParseException exception) {
                    validationErrors.add("Fataler Fehler: " + exception.getMessage());
                }
            });

            // Validate
            validator.validate(new StreamSource(xmlFile));

            if (!validationErrors.isEmpty()) {
                return new ValidationResult(false, validationErrors);
            }

            return new ValidationResult(true, List.of());

        } catch (SAXException e) {
            errors.add("XML Parse Fehler: " + e.getMessage());
            return new ValidationResult(false, errors);
        } catch (IOException e) {
            errors.add("I/O Fehler: " + e.getMessage());
            return new ValidationResult(false, errors);
        }
    }

    /**
     * Validation result
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorsAsString() {
            return String.join("\n", errors);
        }
    }
}
