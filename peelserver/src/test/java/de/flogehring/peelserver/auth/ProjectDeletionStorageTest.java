package de.flogehring.peelserver.auth;

import de.flogehring.peelserver.api.data.print.jobs.PrintJobId;
import de.flogehring.peelserver.config.PeelModuleTest;
import de.flogehring.peelserver.impl.documenttemplates.DocumentId;
import de.flogehring.peelserver.impl.filestorage.StorageService;
import de.flogehring.peelserver.impl.printjobs.PrintJobPersistence;
import de.flogehring.peelserver.impl.printjobs.PrintJobRepository;
import de.flogehring.peelserver.impl.project.ProjectId;
import de.flogehring.peelserver.impl.project.ProjectManagementService;
import de.flogehring.peelserver.impl.project.ProjectPersistence;
import de.flogehring.peelserver.impl.project.ProjectRepository;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import de.flogehring.peelserver.impl.users.UserId;
import de.flogehring.peelserver.impl.users.UserPersistenceData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PeelModuleTest
class ProjectDeletionStorageTest {
    @Autowired
    ProjectManagementService management;
    @Autowired
    ProjectRepository projects;
    @Autowired
    PrintJobRepository jobs;
    @Autowired
    StorageService storage;

    @Test
    void hardDeletionRemovesCsvPdfAndOrphanResultsButPreservesOtherProjectsFiles() {
        var owner = new SpringUserDetails(new UserId("owner"), new UserPersistenceData("owner@example.test", null));
        var project = projects.save(ProjectPersistence.newProject(owner.getUserId(), "Delete me"));
        var job = jobs.save(PrintJobPersistence.init(new PrintJobId(UUID.randomUUID().toString()), new DocumentId("template"), new ProjectId(project.getId()), "Job", "Template"));
        String unrelated = UUID.randomUUID().toString();
        for (String key : new String[]{job.getId(), job.getId() + "_1.pdf", job.getId() + "_orphan.pdf", unrelated})
            storage.uploadFile("print-jobs", key, new ByteArrayInputStream("data".getBytes()), "application/octet-stream");
        management.archive(project.getId(), owner, true);
        management.delete(project.getId(), "Delete me", owner);
        for (String key : new String[]{job.getId(), job.getId() + "_1.pdf", job.getId() + "_orphan.pdf"})
            assertThatThrownBy(() -> storage.downloadFile("print-jobs", key)).isInstanceOf(RuntimeException.class);
        assertThat(storage.downloadFile("print-jobs", unrelated)).isEqualTo("data".getBytes());
        assertThat(projects.existsById(project.getId())).isFalse();
    }
}
