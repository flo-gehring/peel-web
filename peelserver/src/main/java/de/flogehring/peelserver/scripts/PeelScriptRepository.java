package de.flogehring.peelserver.scripts;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeelScriptRepository extends MongoRepository<PeelScript, String> {
}
