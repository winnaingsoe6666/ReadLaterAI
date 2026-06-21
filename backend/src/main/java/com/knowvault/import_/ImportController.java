package com.knowvault.import_;

import com.knowvault.import_.dto.ImportResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/facebook")
    public ResponseEntity<ImportResult> importFacebookArchive(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "deleteAfterImport", defaultValue = "true") boolean deleteAfterImport) {
        ImportResult result = importService.importFacebookArchive(file);
        return ResponseEntity.ok(result);
    }
}
