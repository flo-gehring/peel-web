package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.print.jobs.PrintJobId;
import de.flogehring.peelserver.api.data.print.jobs.PrintJobInitRequestDto;
import de.flogehring.peelserver.api.data.print.jobs.PrintJobStatus;
import de.flogehring.peelserver.api.data.print.jobs.PrintJobSummary;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequestMapping(path = "/api/print-jobs", produces = MediaType.APPLICATION_JSON_VALUE)
public interface PrintJobController {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    PrintJobId initPrintJob(@RequestBody PrintJobInitRequestDto initRequestDto);

    @PutMapping(path = "/{id:.+}/data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    void uploadFile(
            @PathVariable("id") String printJobId,
            @RequestPart("file") MultipartFile multipartFile
    ) throws IOException;

    @GetMapping("/list")
    List<PrintJobSummary> listPrintJobs();

    @PostMapping("/{id:.+}/run")
    PrintJobStatus runPrintJob(@PathVariable("id") String printJobId);

    @GetMapping(path = "/{id:.+}/data", produces = "text/csv")
    byte[] downloadFile(@PathVariable("id") String printJobId);

    @GetMapping(path = "/{id:.+}/file/{fileId:.+}", produces = MediaType.APPLICATION_PDF_VALUE)
    byte[] downloadFile(@PathVariable("id") String printJobId, @PathVariable("fileId") String fileId);
}
