package de.flogehring.peelserver.impl.printjobs;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PrintJobRepository extends MongoRepository<PrintJobPersistence, String> {
}
