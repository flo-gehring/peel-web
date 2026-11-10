package de.flogehring.peelserver.impl.printjobs;

import de.flogehring.peelserver.api.services.PrintJobController;
import de.flogehring.peelserver.api.data.ProjectIdApi;
import de.flogehring.peelserver.api.data.print.jobs.*;
import de.flogehring.peelserver.impl.documenttemplates.DocumentId;
import de.flogehring.peelserver.impl.documenttemplates.DocumentReferenceService;
import de.flogehring.peelserver.impl.documenttemplates.PeelDocumentRepository;
import de.flogehring.peelserver.impl.filestorage.StorageService;
import de.flogehring.peelserver.impl.project.ProjectAccessService;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PrintJobService implements PrintJobController {
    public static final String BUCKET_NAME = "print-jobs";
    private final PrintJobRepository jobs;
    private final StorageService storage;
    private final PrintJobRunner runner;
    private final PeelDocumentRepository documents;
    private final DocumentReferenceService references;
    private final ProjectAccessService projectAccess;

    @Override
    public PrintJobId initPrintJob(PrintJobInitRequestDto request, SpringUserDetails user) {
        var document = documents.findById(request.documentId().id()).orElseThrow(ProjectAccessService::notFound);
        projectAccess.requireProjectAccess(document.getProjectId(), user.getUserId());
        var id = new PrintJobId(UUID.randomUUID().toString());
        jobs.insert(PrintJobPersistence.init(id, request.documentId(), document.getProjectId(), request.name()));
        return id;
    }

    @Override
    public void uploadFile(String id, MultipartFile file, SpringUserDetails user) throws IOException {
        var job = requireJobAccess(id, user);
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("only CSV files can be uploaded");
        }
        if (!"text/csv".equals(file.getContentType())) throw new IllegalArgumentException("CSV uploads must use content type text/csv");
        storage.uploadFile(BUCKET_NAME, id, file.getInputStream(), file.getContentType());
        job.updateFileName(fileName);
        jobs.save(job);
    }

    @Override
    public List<PrintJobSummary> listPrintJobs(SpringUserDetails user) {
        var projectIds = projectAccess.accessibleProjectIds(user.getUserId());
        if (projectIds.isEmpty()) return List.of();
        return jobs.findByProjectIdIn(projectIds).stream().map(job -> {
            var data = job.getPrintJobPersistenceData();
            return new PrintJobSummary(new PrintJobId(job.getId()), new ProjectIdApi(job.getProjectId().id()), data.name(),
                    new DocumentId(data.documentId()), data.fileName(), data.status(), List.copyOf(data.fileIds()));
        }).toList();
    }

    @Override
    public PrintJobStatus runPrintJob(String id, SpringUserDetails user) {
        var job = requireJobAccess(id, user);
        var status = job.getPrintJobPersistenceData().status();
        if (status != PrintJobStatus.CREATED) return status;
        var document = documents.findById(job.getPrintJobPersistenceData().documentId())
                .filter(value -> job.getProjectId().equals(value.getProjectId())).orElseThrow(ProjectAccessService::notFound);
        // Resolve and authorize before scheduling; the worker does not depend on a request SecurityContext.
        var resolved = references.resolve(document);
        runner.run(job, resolved);
        return PrintJobStatus.CALCULATING;
    }

    @Override
    public byte[] downloadFile(String id, SpringUserDetails user) {
        var job = requireJobAccess(id, user);
        if (job.getPrintJobPersistenceData().fileName() == null) throw ProjectAccessService.notFound();
        return storage.downloadFile(BUCKET_NAME, id);
    }

    @Override
    public byte[] downloadFile(String id, String fileId, SpringUserDetails user) {
        var job = requireJobAccess(id, user);
        if (!job.getPrintJobPersistenceData().fileIds().contains(fileId)) throw ProjectAccessService.notFound();
        return storage.downloadFile(BUCKET_NAME, fileId);
    }

    private PrintJobPersistence requireJobAccess(String id, SpringUserDetails user) {
        var job = jobs.findById(id).orElseThrow(ProjectAccessService::notFound);
        projectAccess.requireProjectAccess(job.getProjectId(), user.getUserId());
        return job;
    }
}
