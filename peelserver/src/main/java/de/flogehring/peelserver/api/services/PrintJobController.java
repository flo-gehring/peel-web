package de.flogehring.peelserver.api.services;

import de.flogehring.peelserver.api.data.print.jobs.PrintJobId;
import de.flogehring.peelserver.api.data.print.jobs.PrintJobInitRequestDto;
import de.flogehring.peelserver.api.data.print.jobs.PrintJobStatus;
import de.flogehring.peelserver.api.data.print.jobs.PrintJobSummary;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.io.IOException;
import java.util.List;

@HttpExchange("/api/print-jobs")
public interface PrintJobController {

    @PutExchange("/init")
    PrintJobId initPrintJob(@RequestBody PrintJobInitRequestDto initRequestDto);

    @PutExchange(value = "/{id:.+}/data", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    void uploadFile(
            @PathVariable("id") String printJobId,
            @RequestPart("file") MultipartFile multipartFile
    ) throws IOException;

    @GetExchange("/list")
    List<PrintJobSummary> listPrintJobs();

    @PostExchange("/{id:.+}/run")
    PrintJobStatus runPrintJob(@PathVariable("id") String printJobId);

    @GetExchange("/{id:.+}/data")
    byte[] downloadFile(@PathVariable("id") String printJobId);

    @GetExchange("/{id:.+}/file/{fileId:.+}")
    byte[] downloadFile(@PathVariable("id") String printJobId, @PathVariable("fileId") String fileId);
}
