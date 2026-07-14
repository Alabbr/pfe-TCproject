package org.example.gestionrh.tcproject.Controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.request.LoginRequest;
import org.example.gestionrh.tcproject.Dtos.response.AuthResponse;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Authentifie l'utilisateur avec son email et mot de passe, retourne le token JWT et les informations de session
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Retourne les données à jour de l'utilisateur connecté (permissions, rôle, etc.) sans nécessiter une reconnexion.
     * Le frontend appelle cette méthode au rechargement de la page pour rafraîchir les permissions.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        // Extract existing token from the Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : "";

        return ResponseEntity.ok(authService.refreshCurrentUser(user, token));
    }
}
