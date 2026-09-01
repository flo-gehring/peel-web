package de.flogehring.peelserver;

import de.flogehring.peelserver.api.PrintJobId;
import de.flogehring.peelserver.api.PrintJobInitRequestDto;
import de.flogehring.peelserver.api.PrintJobSummary;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.io.IOException;
import java.util.List;

@HttpExchange("/api/print-jobs")
public interface PrintJobController {

    @PutExchange("/init")
    PrintJobId initPrintJob(@RequestBody PrintJobInitRequestDto initRequestDto);

    @PutExchange("/{id:.+}/data")
    void uploadFile(String printJobId, @RequestParam("file") MultipartFile multipartFile) throws IOException;

    @GetExchange("/list")
    List<PrintJobSummary> listPrintJobs();

    @GetExchange("/{id:.+}/data")
    byte[] downloadFile(String printJobId) throws IOException;
}
