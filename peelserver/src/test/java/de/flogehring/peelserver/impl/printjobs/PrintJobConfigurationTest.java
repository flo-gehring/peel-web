package de.flogehring.peelserver.impl.printjobs;

import de.flogehring.peelserver.api.data.print.jobs.PrintJobId;
import de.flogehring.peelserver.api.data.print.jobs.PrintJobStatus;
import de.flogehring.peelserver.impl.documenttemplates.Document;
import de.flogehring.peelserver.impl.documenttemplates.DocumentId;
import de.flogehring.peelserver.impl.documenttemplates.DocumentTemplateRenderService;
import de.flogehring.peelserver.impl.filestorage.StorageService;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static de.flogehring.peelserver.DocumentFixtures.textDocument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PrintJobConfigurationTest {
    @Test
    void runsJobsInBackgroundAndRejectsASecondJobWhileBusy() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var storage = mock(StorageService.class);
        var jobs = mock(PrintJobRepository.class);
        doAnswer(call -> {
            started.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test did not release worker");
            return null;
        }).when(storage).processFile(anyString(), anyString(), any());

        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(PrintJobConfiguration.class, PrintJobRunner.class);
            var beans = context.getBeanFactory();
            beans.registerSingleton("mongoTemplate", mock(MongoTemplate.class));
            beans.registerSingleton("storageService", storage);
            beans.registerSingleton("printJobRepository", jobs);
            beans.registerSingleton("documentTemplateRenderService", mock(DocumentTemplateRenderService.class));
            context.refresh();
            var runner = context.getBean(PrintJobRunner.class);
            var document = new Document("Template", Map.of(), textDocument(), ExpressionRenderConfiguration.defaultConfig());
            var first = job("first");
            try {
                runner.run(first, document);
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> runner.run(job("second"), document))
                        .isInstanceOf(TaskRejectedException.class);
            } finally {
                release.countDown();
            }
        }
        verify(jobs).save(any(PrintJobPersistence.class));
    }

    private static PrintJobPersistence job(String id) {
        var job = PrintJobPersistence.init(new PrintJobId(id), new DocumentId("template"),
                new ProjectId("project"), "Job", "Template");
        job.updateFileName("data.csv");
        job.updateStatus(PrintJobStatus.CALCULATING);
        return job;
    }
}
