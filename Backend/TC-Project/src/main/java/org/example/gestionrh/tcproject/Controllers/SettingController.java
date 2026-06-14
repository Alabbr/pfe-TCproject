package org.example.gestionrh.tcproject.Controllers;

import org.example.gestionrh.tcproject.Entities.AppSetting;
import org.example.gestionrh.tcproject.Services.AppSettingService;
import org.example.gestionrh.tcproject.Services.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*")
public class SettingController {

    private final AppSettingService appSettingService;
    private final CloudinaryService cloudinaryService;

    public SettingController(AppSettingService appSettingService, CloudinaryService cloudinaryService) {
        this.appSettingService = appSettingService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> getSetting(@PathVariable String key) {
        String value = appSettingService.getSettingValue(key);
        // Return as JSON object
        return ResponseEntity.ok(Map.of("key", key, "value", value != null ? value : ""));
    }

    @PostMapping("/login-background")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> uploadLoginBackground(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = cloudinaryService.uploadImage(file);
            AppSetting setting = appSettingService.updateSetting("LOGIN_BACKGROUND_URL", imageUrl);
            return ResponseEntity.ok(Map.of("key", setting.getSettingKey(), "value", setting.getSettingValue()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error uploading image: " + e.getMessage()));
        }
    }
}
