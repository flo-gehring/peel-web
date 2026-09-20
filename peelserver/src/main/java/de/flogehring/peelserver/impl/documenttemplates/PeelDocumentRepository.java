package de.flogehring.peelserver.impl.documenttemplates;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeelDocumentRepository extends MongoRepository<DocumentPersistence, String> {
}
