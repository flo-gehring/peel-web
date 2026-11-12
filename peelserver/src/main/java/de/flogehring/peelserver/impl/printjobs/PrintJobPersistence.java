package de.flogehring.peelserver.impl.printjobs;

import de.flogehring.peelserver.api.data.print.jobs.PrintJobId;
import de.flogehring.peelserver.api.data.print.jobs.PrintJobStatus;
import de.flogehring.peelserver.impl.documenttemplates.DocumentId;
import de.flogehring.peelserver.impl.project.ProjectId;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("printjobs")
@Getter
public class PrintJobPersistence {

    @Id
    @Indexed(unique = true)
    private final String id;
    private final ProjectId projectId;
    private final String documentName;
    private final java.time.Instant createdAt;
    private PrintJobPersistenceData printJobPersistenceData;

    private PrintJobPersistence(String id, ProjectId projectId, String documentName, java.time.Instant createdAt, PrintJobPersistenceData printJobPersistenceData) {
        this.id = id;
        this.projectId = projectId;
        this.documentName = documentName;
        this.createdAt = createdAt;
        this.printJobPersistenceData = printJobPersistenceData;
    }

    public static PrintJobPersistence init(PrintJobId printJobId, DocumentId documentId, ProjectId projectId, String name, String documentName) {
        return new PrintJobPersistence(
                printJobId.id(),
                projectId,
                documentName,
                java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                new PrintJobPersistenceData(
                        name,
                        documentId.id(),
                        null,
                        PrintJobStatus.CREATED,
                        List.of()
                )
        );
    }

    public void updateFileName(String fileName) {
        this.printJobPersistenceData = new PrintJobPersistenceData(
                this.printJobPersistenceData.name(),
                this.printJobPersistenceData.documentId(),
                fileName,
                this.printJobPersistenceData.status(),
                this.printJobPersistenceData.fileIds()
        );
    }

    public void updateStatus(PrintJobStatus status) {
        this.printJobPersistenceData = new PrintJobPersistenceData(
                this.printJobPersistenceData.name(),
                this.printJobPersistenceData.documentId(),
                this.printJobPersistenceData.fileName(),
                status,
                this.printJobPersistenceData.fileIds()
        );
    }

    public void setDocumentIds(List<String> documentIds) {
        this.printJobPersistenceData = new PrintJobPersistenceData(
                this.printJobPersistenceData.name(),
                this.printJobPersistenceData.documentId(),
                this.printJobPersistenceData.fileName(),
                this.printJobPersistenceData.status(),
                documentIds
        );
    }
}
