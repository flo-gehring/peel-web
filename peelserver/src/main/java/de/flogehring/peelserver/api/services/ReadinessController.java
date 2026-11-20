package de.flogehring.peelserver.api.services;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@Profile("prod")
@RequiredArgsConstructor
public class ReadinessController {
    private final MongoTemplate mongo;

    @GetMapping("/health/ready")
    public ResponseEntity<Void> readiness() {
        try {
            mongo.executeCommand(new Document("ping", 1));
            return ResponseEntity.noContent().build();
        } catch (DataAccessException exception) {
            return ResponseEntity.status(503).build();
        }
    }
}
