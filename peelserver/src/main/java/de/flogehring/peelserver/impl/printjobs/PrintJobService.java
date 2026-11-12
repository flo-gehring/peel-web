package de.flogehring.peelserver.impl.printjobs;

import de.flogehring.peelserver.api.services.PrintJobController;
import de.flogehring.peelserver.api.data.ProjectIdApi;
import de.flogehring.peelserver.api.data.print.jobs.*;
import de.flogehring.peelserver.impl.documenttemplates.DocumentId;
import de.flogehring.peelserver.impl.documenttemplates.DocumentReferenceService;
import de.flogehring.peelserver.impl.files.*;
import de.flogehring.peelserver.impl.filestorage.StorageService;
import de.flogehring.peelserver.impl.project.ProjectAccessService;
import de.flogehring.peelserver.impl.project.ProjectId;
import java.util.*;
import java.util.stream.Collectors;
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
    private final ProjectFileRepository documents;
    private final DocumentReferenceService references;
    private final ProjectFileService files;
    private final ProjectAccessService projectAccess;

    public PrintJobId initPrintJob(String projectId, PrintJobInitRequestDto request, SpringUserDetails user) {
        var project = new ProjectId(projectId);
        return projectAccess.active(project, user.getUserId(), () -> {
            var document = files.requireFile(project, request.documentId().id());
            if (!(document.content() instanceof FileContent.DocumentContent)) throw ProjectAccessService.notFound();
            var id = new PrintJobId(UUID.randomUUID().toString());
            jobs.insert(PrintJobPersistence.init(id, request.documentId(), project, request.name().strip(), document.name()));
            return id;
        });
    }

    public ProjectPrintJobs projectJobs(String projectId, SpringUserDetails user) {
        var project = projectAccess.requireProjectAccess(new ProjectId(projectId), user.getUserId());
        var summaries = jobs.findByProjectId(new ProjectId(projectId)).stream().map(this::summary)
                .sorted(Comparator.comparing(PrintJobSummary::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(job -> job.printJobId().id())).toList();
        var byDocument = summaries.stream().collect(Collectors.groupingBy(job -> job.documentId().id()));
        var existing = files.list(projectId, "document", user).stream()
                .sorted(Comparator.comparing(file -> file.name().toLowerCase(Locale.ROOT)))
                .map(file -> new ProjectPrintJobs.DocumentJobs(file.id(), file.name(), byDocument.getOrDefault(file.id(), List.of()))).toList();
        var existingIds = existing.stream().map(ProjectPrintJobs.DocumentJobs::documentId).collect(Collectors.toSet());
        var deleted = byDocument.entrySet().stream().filter(entry -> !existingIds.contains(entry.getKey()))
                .map(entry -> new ProjectPrintJobs.DocumentJobs(entry.getKey(), entry.getValue().stream()
                        .map(PrintJobSummary::documentName).filter(Objects::nonNull).filter(name -> !name.isBlank())
                        .findFirst().orElse("Gelöschte Dokumentvorlage"), entry.getValue()))
                .sorted(Comparator.comparing(group -> group.name().toLowerCase(Locale.ROOT))).toList();
        return new ProjectPrintJobs(new ProjectIdApi(projectId), project.getPersistenceData().projectName(), project.getState(), existing, deleted);
    }

    public ProjectPrintJobDetails details(String projectId, String id, SpringUserDetails user) {
        var project = projectAccess.requireProjectAccess(new ProjectId(projectId), user.getUserId());
        var job = jobs.findById(id).filter(value -> value.getProjectId().id().equals(projectId)).orElseThrow(ProjectAccessService::notFound);
        var currentDocument = documents.findById(job.getPrintJobPersistenceData().documentId())
                .filter(file -> file.projectId().equals(job.getProjectId()) && file.content() instanceof FileContent.DocumentContent);
        return new ProjectPrintJobDetails(summary(job), project.getPersistenceData().projectName(), project.getState(), currentDocument.map(ProjectFile::name).orElse(null));
    }

    private PrintJobSummary summary(PrintJobPersistence job) {
        var data = job.getPrintJobPersistenceData();
        return new PrintJobSummary(new PrintJobId(job.getId()), new ProjectIdApi(job.getProjectId().id()), data.name(),
                new DocumentId(data.documentId()), job.getDocumentName(), job.getCreatedAt(), data.fileName(), data.status(), List.copyOf(data.fileIds()));
    }

    @Override
    public void uploadFile(String id, MultipartFile file, SpringUserDetails user) throws IOException {
        projectAccess.active(requireJobAccess(id, user).getProjectId(), user.getUserId(), () -> {
            var job = requireJobAccess(id, user);
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
                throw new IllegalArgumentException("only CSV files can be uploaded");
            }
            if (!"text/csv".equals(file.getContentType())) throw new IllegalArgumentException("CSV uploads must use content type text/csv");
            try { storage.uploadFile(BUCKET_NAME, id, file.getInputStream(), file.getContentType()); } catch (IOException exception) { throw new java.io.UncheckedIOException(exception); }
            job.updateFileName(fileName);
            jobs.save(job);
            return null;
        });
    }

    @Override
    public PrintJobStatus runPrintJob(String id, SpringUserDetails user) {
        return projectAccess.active(requireJobAccess(id, user).getProjectId(), user.getUserId(), () -> {
            var job = requireJobAccess(id, user);
            var status = job.getPrintJobPersistenceData().status();
            if (status != PrintJobStatus.CREATED) return status;
            var document = documents.findById(job.getPrintJobPersistenceData().documentId())
                    .filter(value -> job.getProjectId().equals(value.projectId()) && value.content() instanceof FileContent.DocumentContent).orElseThrow(ProjectAccessService::notFound);
            // Resolve and authorize before scheduling; the worker does not depend on a request SecurityContext.
            var resolved = references.resolve(document);
            job.updateStatus(PrintJobStatus.CALCULATING);
            jobs.save(job);
            try {
            runner.run(job, resolved);
            } catch (RuntimeException exception) {
                job.updateStatus(PrintJobStatus.FAILED);
                jobs.save(job);
                throw exception;
            }
            return PrintJobStatus.CALCULATING;
        });
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
