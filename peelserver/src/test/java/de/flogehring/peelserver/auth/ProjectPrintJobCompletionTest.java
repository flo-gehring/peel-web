package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.api.data.print.jobs.PrintJobId;
import de.flogehring.peelserver.api.data.print.jobs.PrintJobStatus;
import de.flogehring.peelserver.impl.documenttemplates.Document;
import de.flogehring.peelserver.impl.documenttemplates.DocumentId;
import de.flogehring.peelserver.impl.documenttemplates.DocumentTemplateRenderService;
import de.flogehring.peelserver.impl.filestorage.StorageService;
import de.flogehring.peelserver.impl.printjobs.PrintJobPersistence;
import de.flogehring.peelserver.impl.printjobs.PrintJobRepository;
import de.flogehring.peelserver.impl.printjobs.PrintJobRunner;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

import static de.flogehring.peelserver.DocumentFixtures.textDocument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProjectPrintJobCompletionTest {
    @Test
    void closingTheCsvMustFinishBeforeTheDatabaseMarksAJobAsDeletable() {
        var jobs = mock(PrintJobRepository.class);
        var storage = mock(StorageService.class);
        var renderer = mock(DocumentTemplateRenderService.class);
        var runner = new PrintJobRunner(jobs, storage, renderer);
        var job = PrintJobPersistence.init(new PrintJobId("job"), new DocumentId("template"), new ProjectId("project"), "Job", "Template");
        job.updateFileName("data.csv");
        job.updateStatus(PrintJobStatus.CALCULATING);
        doAnswer(invocation -> {
            Consumer<InputStream> process = invocation.getArgument(2);
            process.accept(new ByteArrayInputStream("a\n1\n".getBytes()));
            verifyNoInteractions(jobs);
            throw new IllegalStateException("stream close failed");
        }).when(storage).processFile(anyString(), anyString(), any());
        runner.run(job, new Document("Template", Map.of(), textDocument(), ExpressionRenderConfiguration.defaultConfig()));
        verify(jobs, times(1)).save(job);
        assertThat(job.getPrintJobPersistenceData().status()).isEqualTo(PrintJobStatus.FAILED);
    }
}
