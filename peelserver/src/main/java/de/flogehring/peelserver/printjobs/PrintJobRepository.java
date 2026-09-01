package de.flogehring.peelserver.printjobs;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PrintJobRepository extends MongoRepository<PrintJobPersistence, String> {
}
