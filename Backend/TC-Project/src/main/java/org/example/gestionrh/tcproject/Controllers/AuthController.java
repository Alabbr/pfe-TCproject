package org.example.gestionrh.tcproject.Controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.request.LoginRequest;
import org.example.gestionrh.tcproject.Dtos.response.AuthResponse;
import org.example.gestionrh.tcproject.Services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
