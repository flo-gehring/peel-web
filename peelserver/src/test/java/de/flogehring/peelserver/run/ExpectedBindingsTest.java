package de.flogehring.peelserver.run;

import de.flogehring.peelserver.impl.run.BindingInputException;
import de.flogehring.peelserver.impl.run.ExpectedBinding;
import de.flogehring.peelserver.impl.run.ExpectedBindings;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ExpectedBindingsTest {
    @Test void reportsAllMissingInputsButAcceptsNullAndExtraValues() {
        var expected = List.of(new ExpectedBinding("amount", ""), new ExpectedBinding("customer", ""));
        var values = new HashMap<String, Object>();
        values.put("amount", null);
        values.put("extra", 42);
        assertThatThrownBy(() -> ExpectedBindings.require(expected, values.keySet()))
                .isInstanceOfSatisfying(BindingInputException.class,
                        error -> assertThat(error.details().get("missingBindings")).isEqualTo(List.of("customer")));
        values.put("customer", Map.of());
        assertThatCode(() -> ExpectedBindings.require(expected, values.keySet())).doesNotThrowAnyException();
    }

    @Test void rejectsInvalidReservedAndDuplicateNames() {
        for (var name : Arrays.asList("", "a-b", "var", "True", "while"))
            assertThatThrownBy(() -> ExpectedBindings.validate(List.of(new ExpectedBinding(name, ""))))
                    .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpectedBindings.validate(List.of(
                new ExpectedBinding("x", ""), new ExpectedBinding("x", ""))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
