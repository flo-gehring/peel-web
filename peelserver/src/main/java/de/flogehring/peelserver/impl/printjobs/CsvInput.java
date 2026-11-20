package de.flogehring.peelserver.impl.printjobs;

import de.flogehring.peelserver.impl.documenttemplates.Document;
import de.flogehring.peelserver.impl.run.BindingInputException;
import de.flogehring.peelserver.impl.run.ExpectedBindings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.DuplicateHeaderMode;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class CsvInput {
    private static final int MAX_COLUMNS = 100;
    private static final int MAX_ROWS = 500;
    private static final CSVFormat FORMAT = CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true)
            .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).setAllowMissingColumnNames(false).build();
    private CsvInput() {}

    public static CSVParser open(InputStream stream) throws IOException {
        return CSVParser.parse(stream, UTF_8, FORMAT);
    }

    public static void validate(InputStream stream, Document document) {
        try (var parser = open(stream)) {
            if (parser.getHeaderNames().isEmpty()) throw new IllegalArgumentException("CSV header is empty");
            if (parser.getHeaderNames().size() > MAX_COLUMNS)
                throw new BindingInputException("invalid_csv", "A CSV batch can contain at most 100 columns", Map.of());
            ExpectedBindings.requireDocument(document, parser.getHeaderNames(), true);
            for (var row : parser) {
                if (row.getRecordNumber() > MAX_ROWS)
                    throw new BindingInputException("invalid_csv", "A CSV batch can contain at most 500 rows", Map.of());
                if (!row.isConsistent())
                    throw new BindingInputException("invalid_csv", "Each CSV row must match the header width", Map.of());
            }
        } catch (BindingInputException exception) {
            throw exception;
        } catch (IOException | java.io.UncheckedIOException | IllegalArgumentException exception) {
            throw new BindingInputException("invalid_csv", "The CSV could not be read", Map.of());
        }
    }
}
