package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.print.jobs.*;
import de.flogehring.peelserver.impl.printjobs.PrintJobService;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/projects/{projectId}/print-jobs", produces = "application/json")
public class ProjectPrintJobController {
    private final PrintJobService service;
    @GetMapping
    public ProjectPrintJobs list(@PathVariable String projectId, @AuthenticationPrincipal SpringUserDetails user) {
        return service.projectJobs(projectId, user);
    }
    @PostMapping
    public PrintJobId create(@PathVariable String projectId, @Valid @RequestBody PrintJobInitRequestDto request,
                             @AuthenticationPrincipal SpringUserDetails user) {
        return service.initPrintJob(projectId, request, user);
    }
    @GetMapping("/{id}")
    public ProjectPrintJobDetails details(@PathVariable String projectId, @PathVariable String id,
                                        @AuthenticationPrincipal SpringUserDetails user) {
        return service.details(projectId, id, user);
    }
}
