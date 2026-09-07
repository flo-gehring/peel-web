package de.flogehring.peelserver.printjobs;

import de.flogehring.peelserver.api.PrintJobStatus;
import de.flogehring.peelserver.documents.*;
import de.flogehring.peelserver.error.ResourceNotFoundException;
import de.flogehring.peelserver.filestorage.StorageService;
import de.flogehring.peelserver.renderconfig.RenderConfigurationRepository;
import de.flogehring.peelserver.scripts.PeelScript;
import de.flogehring.peelserver.scripts.PeelScriptId;
import de.flogehring.peelserver.scripts.PeelScriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.jspecify.annotations.NonNull;
import org.openpdf.pdf.ITextRenderer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.flogehring.peelserver.util.StreamUtil.transformMapValues;
import static java.nio.charset.StandardCharsets.UTF_8;


@Async
@Component
@RequiredArgsConstructor
@Slf4j
public class PrintJobRunner {

    private final PrintJobRepository printJobRepository;
    private final StorageService storageService;
    private final PeelDocumentRepository documentRepository;
    private final DocumentRenderService documentRenderService;
    private final RenderConfigurationRepository renderConfigurationRepository;
    private final PeelScriptRepository scriptRepository;

    void run(PrintJobPersistence printJob) {
        PrintJobStatus status = printJob.getPrintJobPersistenceData().status();
        if (status != PrintJobStatus.CREATED) {
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
        if (fileName == null) {
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
        DocumentPersistenceData data = documentRepository.findById(printJobPersistence.getPrintJobPersistenceData().documentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + printJobPersistence.getPrintJobPersistenceData().documentId())).getData();

        Document document = new Document(
                data.name(),
                transformMapValues(data.scriptNameTags(), this::getPeelScript),
                data.templateHtml(),
                renderConfigurationRepository.findById(data.renderConfigurationId().id()).orElseThrow(() -> new ResourceNotFoundException("Render configuration not found: " + data.renderConfigurationId().id())).getExpressionRenderConfiguration()
        );
        try (CSVParser csvParser = CSVParser.parse(resource, UTF_8, CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true).build())) {
            // Process CSV records
            List<String> documentIds = new ArrayList<>();
            for (var record : csvParser) {
                String html = documentRenderService.render(
                        document,
                        csvParser.getHeaderNames()
                                .stream()
                                .map(header -> Map.entry(header, record.get(header)))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                                )
                );
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    ITextRenderer renderer = new ITextRenderer();
                    renderer.setDocumentFromString(html);
                    renderer.layout();
                    renderer.createPDF(outputStream);
                    outputStream.flush();
                    String objectId = printJobPersistence.getId() + "_" + record.getRecordNumber() + ".pdf";
                    documentIds.add(objectId);
                    storageService.uploadFile(
                            PrintJobService.BUCKET_NAME,
                            objectId,
                            new ByteArrayInputStream(outputStream.toByteArray()),
                            "application/pdf"
                    );
                }
            }
            printJobPersistence.setDocumentIds(documentIds);
            printJobPersistence.updateStatus(PrintJobStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Error processing CSV for print job {}: {}", printJobPersistence.getId(), e.getMessage());
            printJobPersistence.updateStatus(PrintJobStatus.FAILED);
        } finally {
            printJobRepository.save(printJobPersistence);
        }
    }

    private @NonNull PeelScript getPeelScript(PeelScriptId scriptNameTag) {
        return scriptRepository.findById(scriptNameTag.id())
                .orElseThrow(() -> new ResourceNotFoundException("Script not found: " + scriptNameTag))
                .getPeelScript();
    }

}
