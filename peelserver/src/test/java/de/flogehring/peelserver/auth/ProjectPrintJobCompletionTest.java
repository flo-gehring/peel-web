package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.impl.printjobs.*;
import de.flogehring.peelserver.impl.documenttemplates.*;
import de.flogehring.peelserver.impl.filestorage.StorageService;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.api.data.print.jobs.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.Map;
import java.util.function.Consumer;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ProjectPrintJobCompletionTest {
    @Test void closingTheCsvMustFinishBeforeTheDatabaseMarksAJobAsDeletable() {
        var jobs = mock(PrintJobRepository.class);
        var storage = mock(StorageService.class);
        var renderer = mock(DocumentTemplateRenderService.class);
        var runner = new PrintJobRunner(jobs, storage, renderer);
        var job = PrintJobPersistence.init(new PrintJobId("job"), new DocumentId("template"), new ProjectId("project"), "Job");
        job.updateFileName("data.csv"); job.updateStatus(PrintJobStatus.CALCULATING);
        doAnswer(invocation -> {
            Consumer<InputStream> process = invocation.getArgument(2);
            process.accept(new ByteArrayInputStream("a\n1\n".getBytes()));
            verifyNoInteractions(jobs);
            throw new IllegalStateException("stream close failed");
        }).when(storage).processFile(anyString(), anyString(), any());
        runner.run(job, new Document("Template", Map.of(), "<p>Test</p>", ExpressionRenderConfiguration.defaultConfig()));
        verify(jobs, times(1)).save(job);
        assertThat(job.getPrintJobPersistenceData().status()).isEqualTo(PrintJobStatus.FAILED);
    }
}
