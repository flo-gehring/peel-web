package de.flogehring.peelserver.documents;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeelDocumentRepository extends MongoRepository<DocumentPersistence, String> {
}
