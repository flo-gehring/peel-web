package de.flogehring.peelserver.api.services;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(path = "/ott", produces = "application/json")
public interface AuthController {

    @GetMapping("/sent")
     String ottSent();
}
