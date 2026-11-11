package de.flogehring.peelserver.impl.printjobs;

import de.flogehring.peelserver.api.data.print.jobs.PrintJobStatus;
import de.flogehring.peelserver.impl.documenttemplates.Document;
import de.flogehring.peelserver.impl.documenttemplates.DocumentTemplateRenderService;
import de.flogehring.peelserver.impl.filestorage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;


@Async
@Component
@RequiredArgsConstructor
@Slf4j
public class PrintJobRunner {

    public static final Pattern INTEGER = Pattern.compile("^\\d+$");
    public static final Pattern FLOAT = Pattern.compile("^\\d+\\.\\d+$");
    private final PrintJobRepository printJobRepository;
    private final StorageService storageService;
    private final DocumentTemplateRenderService documentTemplateRenderService;

    public void run(PrintJobPersistence printJob, Document document) {
        PrintJobStatus status = printJob.getPrintJobPersistenceData().status();
        if (status != PrintJobStatus.CALCULATING) {
            log.warn("Print job {} is not in CALCULATING state, current state: {}", printJob.getId(), status);
            return;
        }
        try { runJob(printJob, document); }
        catch (RuntimeException exception) {
            printJob.updateStatus(PrintJobStatus.FAILED);
            log.error("Print job {} failed", printJob.getId(), exception);
        } finally {
            // Publish a terminal status only after storage processing and stream closing have finished.
            printJobRepository.save(printJob);
        }
    }

    private void runJob(PrintJobPersistence printJobPersistence, Document document) {
        String fileName = printJobPersistence.getPrintJobPersistenceData().fileName();
        if (fileName == null) {
            log.warn("Print job {} has no file uploaded, cannot run", printJobPersistence.getId());
            printJobPersistence.updateStatus(PrintJobStatus.FAILED);
            return;
        }
        storageService.processFile(
                PrintJobService.BUCKET_NAME,
                printJobPersistence.getId(),
                resource -> processCsv(resource, printJobPersistence, document)
        );
    }

    private void processCsv(InputStream resource, PrintJobPersistence printJobPersistence, Document document) {
        try (CSVParser csvParser = CSVParser.parse(resource, UTF_8, CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true).build())) {
            // Process CSV records
            List<String> documentIds = new ArrayList<>();
            for (var record : csvParser) {
                Map<String, Object> context = csvParser.getHeaderNames()
                        .stream()
                        .map(header -> Map.entry(header, tryInferData(record.get(header))))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                        );
                documentTemplateRenderService.renderPdf(
                        document,
                        context,
                        outputStream -> {
                            String objectId = printJobPersistence.getId() + "_" + record.getRecordNumber() + ".pdf";
                            documentIds.add(objectId);
                            storageService.uploadFile(
                                    PrintJobService.BUCKET_NAME,
                                    objectId,
                                    new ByteArrayInputStream(outputStream.toByteArray()),
                                    "application/pdf"
                            );
                            return (Void) null;
                        }
                );
            }
            printJobPersistence.setDocumentIds(documentIds);
            printJobPersistence.updateStatus(PrintJobStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Error processing CSV for print job {}: {}", printJobPersistence.getId(), e.getMessage());
            printJobPersistence.updateStatus(PrintJobStatus.FAILED);
        }
    }

    private Object tryInferData(String s) {
        if(INTEGER.matcher(s).matches()) {
            return Integer.parseInt(s);
        } else if(FLOAT.matcher(s).matches()) {
            return Double.parseDouble(s);
        } else if(Pattern.compile("^(true|false)$", Pattern.CASE_INSENSITIVE).matcher(s).matches()) {
            return Boolean.parseBoolean(s);
        } else {
            return s;
        }
    }

}
