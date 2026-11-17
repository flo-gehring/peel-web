package de.flogehring.peelserver.run;

import de.flogehring.peelserver.api.data.print.jobs.*;
import de.flogehring.peelserver.impl.printjobs.*;
import de.flogehring.peelserver.impl.documenttemplates.*;
import de.flogehring.peelserver.impl.files.*;
import de.flogehring.peelserver.impl.project.*;
import de.flogehring.peelserver.impl.users.*;
import de.flogehring.peelserver.impl.scripts.PeelScript;
import de.flogehring.peelserver.impl.filestorage.StorageService;
import de.flogehring.peelserver.impl.run.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PrintJobPreflightTest {
    @Test void rejectsMissingColumnsBeforeSavingOrSchedulingAndAcceptsCorrectedInput() {
        var jobs = mock(PrintJobRepository.class);
        var storage = mock(StorageService.class);
        var runner = mock(PrintJobRunner.class);
        var files = mock(ProjectFileRepository.class);
        var references = mock(DocumentReferenceService.class);
        var access = mock(ProjectAccessService.class);
        var user = new SpringUserDetails(new UserId("user"), null);
        var project = new ProjectId("project");
        var job = PrintJobPersistence.init(new PrintJobId("job"), new DocumentId("doc"), project, "Job", "Document");
        job.updateFileName("input.csv");
        var file = new ProjectFile("doc", project, "Document", "", 0,
                new FileContent.DocumentContent(Map.of(), "html", "{}", "config"), null, null);
        var document = new Document("Document", Map.of("calculation",
                new PeelScript("Calculation", "", List.of(new ExpectedBinding("amount", "")))), "", null);
        when(jobs.findById("job")).thenReturn(Optional.of(job));
        when(files.findById("doc")).thenReturn(Optional.of(file));
        when(references.resolve(file)).thenReturn(document);
        when(access.active(any(), any(), any())).thenAnswer(call -> ((Supplier<?>) call.getArgument(2)).get());
        var csv = new java.util.concurrent.atomic.AtomicReference<>("other\n1\n");
        doAnswer(call -> {
            Consumer<java.io.InputStream> consumer = call.getArgument(2);
            consumer.accept(new ByteArrayInputStream(csv.get().getBytes(UTF_8)));
            return null;
        }).when(storage).processFile(eq("print-jobs"), eq("job"), any());
        var service = new PrintJobService(jobs, storage, runner, files, references, mock(ProjectFileService.class), access);
        assertThatThrownBy(() -> service.runPrintJob("job", user)).isInstanceOf(BindingInputException.class);
        assertThat(job.getPrintJobPersistenceData().status()).isEqualTo(PrintJobStatus.CREATED);
        verify(jobs, never()).save(any());
        verifyNoInteractions(runner);
        csv.set("amount\n12\n");
        assertThat(service.runPrintJob("job", user)).isEqualTo(PrintJobStatus.CALCULATING);
        verify(runner).run(job, document);
        assertThatThrownBy(() -> service.uploadFile("job",
                new MockMultipartFile("file", "replacement.csv", "text/csv", new byte[0]), user))
                .isInstanceOf(IllegalArgumentException.class);
        verify(storage, never()).uploadFile(any(), any(), any(), any());
    }
}
