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
import de.flogehring.peelserver.impl.run.PeelExecutionService;
import de.flogehring.peelserver.impl.scripts.PeelScript;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static de.flogehring.peelserver.DocumentFixtures.textDocument;
import static de.flogehring.peelserver.DocumentFixtures.calculation;
import static de.flogehring.peelserver.DocumentFixtures.doc;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProjectPrintJobCompletionTest {
    @Test
    void rendersAnInterestCalculationBatchWithTheBetaLimits() throws Exception {
        var jobs = mock(PrintJobRepository.class);
        var storage = mock(StorageService.class);
        var execution = new PeelExecutionService(Duration.ofSeconds(5), 2, 100_000, 64, 500_000, 65_536);
        var runner = new PrintJobRunner(jobs, storage, new DocumentTemplateRenderService(execution));
        var job = PrintJobPersistence.init(new PrintJobId("job"), new DocumentId("template"), new ProjectId("project"), "Job", "Template");
        job.updateFileName("data.csv");
        job.updateStatus(PrintJobStatus.CALCULATING);

        var script = Files.readString(Path.of("docs/testdata/zinstest/zins.peel"));
        var document = new Document("Compound interest", Map.of("script", new PeelScript("Interest", script, List.of())),
                doc(calculation("script")), ExpressionRenderConfiguration.defaultConfig());
        doAnswer(invocation -> {
            Consumer<InputStream> process = invocation.getArgument(2);
            process.accept(new ByteArrayInputStream("betrag,zins,laufzeit\n1000,0.05,10\n1400,0.05,14\n".getBytes(UTF_8)));
            return null;
        }).when(storage).processFile(anyString(), eq("job"), any());
        doAnswer(invocation -> {
            InputStream pdf = invocation.getArgument(2);
            byte[] bytes = pdf.readAllBytes();
            assertThat(bytes).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
            assertThat(bytes).hasSize(Math.toIntExact(invocation.getArgument(3, Long.class)));
            return null;
        }).when(storage).uploadFile(anyString(), anyString(), any(), anyLong(), eq("application/pdf"));

        runner.run(job, document);

        assertThat(job.getPrintJobPersistenceData().status()).isEqualTo(PrintJobStatus.COMPLETED);
        assertThat(job.getPrintJobPersistenceData().fileIds()).containsExactly("job_1.pdf", "job_2.pdf");
        verify(storage, times(2)).uploadFile(anyString(), anyString(), any(), anyLong(), eq("application/pdf"));
        verify(jobs).save(job);
    }

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
