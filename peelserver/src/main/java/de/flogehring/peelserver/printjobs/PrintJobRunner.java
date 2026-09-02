package de.flogehring.peelserver.printjobs;

import de.flogehring.peelserver.api.PrintJobStatus;
import de.flogehring.peelserver.filestorage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Async
@Component
@RequiredArgsConstructor
@Slf4j
public class PrintJobRunner {

    private final PrintJobRepository printJobRepository;
    private final StorageService storageService;

    void run(PrintJobPersistence printJob) {
        PrintJobStatus status = printJob.getPrintJobPersistenceData().status();
        if(status != PrintJobStatus.CREATED) {
            log.warn("Print job {} is not in CREATED state, current state: {}", printJob.getId(), status);
            printJob.updateStatus(PrintJobStatus.FAILED);
            printJobRepository.save(printJob);
            return;
        }
        printJob.updateStatus(PrintJobStatus.CALCULATING);
        printJobRepository.save(printJob);
        runJob(printJob);
    }

    private void runJob(PrintJobPersistence printJobPersistence) {
        String fileName = printJobPersistence.getPrintJobPersistenceData().fileName();
        if(fileName == null) {
            log.warn("Print job {} has no file uploaded, cannot run", printJobPersistence.getId());
            printJobPersistence.updateStatus(PrintJobStatus.FAILED);
            printJobRepository.save(printJobPersistence);
            return;
        }
        storageService.processFile(
                PrintJobService.BUCKET_NAME,
                printJobPersistence.getId(),
                resource -> processCsv(resource, printJobPersistence)
        );
    }

    private void processCsv(InputStream resource, PrintJobPersistence printJobPersistence) {
        try (CSVParser csvParser = CSVParser.parse(resource, UTF_8, CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true).build())) {
            // Process CSV records
            csvParser.forEach(record -> {
                // Implement your processing logic here
                log.info("Processing record: {}", record);
            });
            printJobPersistence.updateStatus(PrintJobStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Error processing CSV for print job {}: {}", printJobPersistence.getId(), e.getMessage());
            printJobPersistence.updateStatus(PrintJobStatus.FAILED);
        } finally {
            printJobRepository.save(printJobPersistence);
        }
    }

}
