package de.flogehring.peelserver.run;

import de.flogehring.peelserver.impl.run.BindingInputException;
import de.flogehring.peelserver.impl.run.PeelExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeelExecutionServiceTest {
    @Test
    @Timeout(12)
    void reportsTheCoreStepLimitAndReleasesTheOnlyWorkerSlot() {
        var service = new PeelExecutionService(Duration.ofSeconds(5), 1, 50, 64, 10_000, 1_000);

        assertThatThrownBy(() -> service.execute("1 ** 1000;", Map.of()))
                .isInstanceOfSatisfying(BindingInputException.class, error -> {
                    assertThat(error.code()).isEqualTo("script_resource_limit");
                    assertThat(error.details()).containsEntry("limit", "STEP_LIMIT");
                });

        assertThat(service.execute("2 + 3;", Map.of()).value()).containsEntry("value", 5);
    }

    @Test
    @Timeout(12)
    void enforcesTheProcessDeadlineBeforeEvaluationStarts() {
        var service = new PeelExecutionService(Duration.ofMillis(1), 1, 50, 64, 10_000, 1_000);

        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> service.execute("2 + 3;", Map.of()))
                    .isInstanceOfSatisfying(BindingInputException.class, error -> {
                        assertThat(error.code()).isEqualTo("script_timeout");
                        assertThat(error.details()).containsEntry("timeoutMs", 1L);
                    });
        }
    }
}
