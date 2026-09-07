package de.flogehring.peelserver.printjobs;

import de.flogehring.peelserver.api.PrintJobId;
import de.flogehring.peelserver.api.PrintJobStatus;
import de.flogehring.peelserver.documents.DocumentId;
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
    private PrintJobPersistenceData printJobPersistenceData;

    private PrintJobPersistence(String id, PrintJobPersistenceData printJobPersistenceData) {
        this.id = id;
        this.printJobPersistenceData = printJobPersistenceData;
    }

    public static PrintJobPersistence init(PrintJobId printJobId, DocumentId documentId, String name) {
        return new PrintJobPersistence(
                printJobId.id(),
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