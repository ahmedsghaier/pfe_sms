package com.easybulk.aitimingservice.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/api/v1/model-results/chart")
public class ChartImageController {

    private static final String CHART_DIR = "/app/models/charts";

    @GetMapping("/{name}")
    public ResponseEntity<FileSystemResource> getChart(@PathVariable String name) {
        File file = new File(CHART_DIR, name + ".png");
        if (!file.exists()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .lastModified(file.lastModified())          // <-- clé pour le cache
                .cacheControl(CacheControl.noCache())        // revalide à chaque requête, mais ne re-télécharge pas si inchangé
                .body(new FileSystemResource(file));
    }
}