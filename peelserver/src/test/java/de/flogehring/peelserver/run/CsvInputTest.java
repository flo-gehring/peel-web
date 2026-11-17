package de.flogehring.peelserver.run;

import de.flogehring.peelserver.impl.documenttemplates.Document;
import de.flogehring.peelserver.impl.scripts.PeelScript;
import de.flogehring.peelserver.impl.printjobs.CsvInput;
import de.flogehring.peelserver.impl.run.ExpectedBinding;
import de.flogehring.peelserver.impl.run.BindingInputException;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class CsvInputTest {
    private final Document document = new Document("invoice", Map.of(
            "first", new PeelScript("first", "", List.of(new ExpectedBinding("amount", ""))),
            "second", new PeelScript("second", "", List.of(new ExpectedBinding("customer", ""), new ExpectedBinding("amount", "")))
    ), de.flogehring.peelserver.DocumentFixtures.doc(
            de.flogehring.peelserver.DocumentFixtures.calculation("first"),
            de.flogehring.peelserver.DocumentFixtures.calculation("second")), null);

    private void validate(String csv) {
        CsvInput.validate(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), document);
    }

    @Test void requiresUnionOfScriptInputsAndReportsAllSources() {
        assertThatThrownBy(() -> validate("extra\n1\n")).isInstanceOfSatisfying(BindingInputException.class, error -> {
            assertThat(error.code()).isEqualTo("missing_csv_columns");
            assertThat(error.details().get("missingColumns")).isEqualTo(List.of("amount", "customer"));
            assertThat((Map<?, ?>) error.details().get("scripts")).hasSize(2);
        });
    }
    @Test void acceptsExtraColumnsAndEmptyCells() {
        assertThatCode(() -> validate("amount,customer,extra\n,,x\n")).doesNotThrowAnyException();
    }
    @Test void rejectsDuplicateHeadersAndCaseMismatch() {
        assertThatThrownBy(() -> validate("amount,amount,customer\n1,2,x\n")).isInstanceOfSatisfying(
                BindingInputException.class, error -> assertThat(error.code()).isEqualTo("invalid_csv"));
        assertThatThrownBy(() -> validate("Amount,customer\n1,x\n")).isInstanceOfSatisfying(
                BindingInputException.class, error -> assertThat(error.code()).isEqualTo("missing_csv_columns"));
    }
}
