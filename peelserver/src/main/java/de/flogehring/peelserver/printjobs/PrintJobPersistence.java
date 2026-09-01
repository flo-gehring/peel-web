package de.flogehring.peelserver.printjobs;

import de.flogehring.peelserver.api.PrintJobId;
import de.flogehring.peelserver.documents.DocumentId;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
                        null
                )
        );
    }

    public void updateFileName(String fileName) {
        this.printJobPersistenceData = new PrintJobPersistenceData(
                this.printJobPersistenceData.name(),
                this.printJobPersistenceData.documentId(),
                fileName
        );
    }
}
