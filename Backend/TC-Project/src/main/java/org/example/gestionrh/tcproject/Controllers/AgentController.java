package org.example.gestionrh.tcproject.Controllers;

import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Services.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/execute")
    public ResponseEntity<?> executeCommand(@RequestBody Map<String, String> payload, @AuthenticationPrincipal User currentUser) {
        try {
            String command = payload.get("command");
            if (command == null || command.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La commande est vide."));
            }
            Map<String, Object> result = agentService.executeCommand(command, currentUser);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        }
    }
}
