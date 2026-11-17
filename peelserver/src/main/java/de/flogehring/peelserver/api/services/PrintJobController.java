package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.print.jobs.PrintJobStatus;
import de.flogehring.peelserver.impl.users.SpringUserDetails;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequestMapping(path = "/api/print-jobs", produces = MediaType.APPLICATION_JSON_VALUE)
public interface PrintJobController {

    @PutMapping(path = "/{id:.+}/data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    void uploadFile(
            @PathVariable("id") String printJobId,
            @RequestPart("file") MultipartFile multipartFile,
            @AuthenticationPrincipal SpringUserDetails user
    ) throws IOException;

    @PostMapping("/{id:.+}/run")
    PrintJobStatus runPrintJob(@PathVariable("id") String printJobId, @AuthenticationPrincipal SpringUserDetails user);

    @GetMapping(path = "/{id:.+}/data", produces = "text/csv")
    byte[] downloadFile(@PathVariable("id") String printJobId, @AuthenticationPrincipal SpringUserDetails user);

    @GetMapping(path = "/{id:.+}/file/{fileId:.+}", produces = MediaType.APPLICATION_PDF_VALUE)
    byte[] downloadFile(@PathVariable("id") String printJobId, @PathVariable("fileId") String fileId, @AuthenticationPrincipal SpringUserDetails user);
}
