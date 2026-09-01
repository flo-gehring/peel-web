package de.flogehring.peelserver.printjobs;

import de.flogehring.peelserver.PrintJobController;
import de.flogehring.peelserver.api.PrintJobId;
import de.flogehring.peelserver.api.PrintJobInitRequestDto;
import de.flogehring.peelserver.api.PrintJobSummary;
import de.flogehring.peelserver.documents.PeelDocumentRepository;
import de.flogehring.peelserver.filestorage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PrintJobService implements PrintJobController {

    public static final String BUCKET_NAME = "print-jobs";
    private final PrintJobRepository printJobRepository;
    private final PeelDocumentRepository peelDocumentRepository;
    private final StorageService storageService;

    @Override
    public PrintJobId initPrintJob(PrintJobInitRequestDto initRequestDto) {
        PrintJobId printJobIdResponse = new PrintJobId(UUID.randomUUID().toString());
         printJobRepository.insert(
                PrintJobPersistence.init(printJobIdResponse, initRequestDto.documentId(), initRequestDto.name())
        );
         return printJobIdResponse;
    }

    @Override
    public void uploadFile(@NonNull String printJobId, MultipartFile multipartFile) throws IOException {
        PrintJobPersistence printJobPersistence = printJobRepository.findById(printJobId)
                .orElseThrow(() -> new IllegalArgumentException("Print job not found: " + printJobId));
        storageService.uploadFile(
                BUCKET_NAME,
                printJobId,
                multipartFile.getInputStream(),
                multipartFile.getContentType()
        );
        printJobPersistence.updateFileName(multipartFile.getOriginalFilename());
        printJobRepository.save(printJobPersistence);
    }

    @Override
    public List<PrintJobSummary> listPrintJobs() {
        return printJobRepository.findAll().stream()
                .map(printJobPersistence -> new PrintJobSummary(
                        new PrintJobId(printJobPersistence.getId()),
                        printJobPersistence.getPrintJobPersistenceData().name(),
                        // TODO hier hat die KI verkackt, ich will die documentId zurückgeben, nicht den Namen des Dokuments. Ich muss das noch anpassen.
                        peelDocumentRepository.findById(printJobPersistence.getPrintJobPersistenceData().documentId())
                                .map(document -> document.getData().name())
                                .orElse(null),
                        printJobPersistence.getPrintJobPersistenceData().fileName()
                ))
                .toList();
    }

    @Override
    public byte[] downloadFile(String printJobId) throws IOException {
        printJobRepository.findById(printJobId)
                .orElseThrow(() -> new IllegalArgumentException("Print job not found: " + printJobId));
        return storageService.downloadFile(BUCKET_NAME, printJobId);
    }
}
