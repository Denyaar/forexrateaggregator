/**
 * Created by tendaimupezeni for forexrateaggregator
 * Date: 8/14/25
 * Time: 8:12 PM
 */

package co.zw.mupezeni.wiremit.forexrateaggregator.controller;


import co.zw.mupezeni.wiremit.forexrateaggregator.dto.AuthDTOs;
import co.zw.mupezeni.wiremit.forexrateaggregator.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthService authService;

    /**
     * User signup endpoint
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody AuthDTOs.SignupRequest request, BindingResult bindingResult) {
        log.info("Signup attempt for username: {}", request.getUsername());

        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            return ResponseEntity.badRequest().body(
                    AuthDTOs.ErrorResponse.builder()
                            .error("Validation Error")
                            .message(errors)
                            .status(HttpStatus.BAD_REQUEST.value())
                            .timestamp(LocalDateTime.now().toString())
                            .path("/api/v1/auth/signup")
                            .build()
            );
        }

        try {
            AuthDTOs.AuthResponse response = authService.signup(request);
            log.info("Successfully registered user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Signup failed for username: {} - {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    AuthDTOs.ErrorResponse.builder()
                            .error("Registration Error")
                            .message(e.getMessage())
                            .status(HttpStatus.BAD_REQUEST.value())
                            .timestamp(LocalDateTime.now().toString())
                            .path("/api/v1/auth/signup")
                            .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during signup for username: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    AuthDTOs.ErrorResponse.builder()
                            .error("Internal Server Error")
                            .message("An unexpected error occurred during registration")
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .timestamp(LocalDateTime.now().toString())
                            .path("/api/v1/auth/signup")
                            .build()
            );
        }
    }

    /**
     * User login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDTOs.LoginRequest request, BindingResult bindingResult) {
        log.info("Login attempt for username: {}", request.getUsername());

        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            return ResponseEntity.badRequest().body(
                    AuthDTOs.ErrorResponse.builder()
                            .error("Validation Error")
                            .message(errors)
                            .status(HttpStatus.BAD_REQUEST.value())
                            .timestamp(LocalDateTime.now().toString())
                            .path("/api/v1/auth/login")
                            .build()
            );
        }

        try {
            AuthDTOs.AuthResponse response = authService.login(request);
            log.info("Successfully authenticated user: {}", request.getUsername());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Login failed for username: {} - Invalid credentials", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    AuthDTOs.ErrorResponse.builder()
                            .error("Authentication Error")
                            .message("Invalid username or password")
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .timestamp(LocalDateTime.now().toString())
                            .path("/api/v1/auth/login")
                            .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during login for username: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    AuthDTOs.ErrorResponse.builder()
                            .error("Internal Server Error")
                            .message("An unexpected error occurred during authentication")
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .timestamp(LocalDateTime.now().toString())
                            .path("/api/v1/auth/login")
                            .build()
            );
        }
    }

    /**
     * Check username availability
     */
    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsername(@PathVariable String username) {
        log.debug("Checking username availability: {}", username);

        boolean available = authService.isUsernameAvailable(username);

        return ResponseEntity.ok().body(
                new CheckAvailabilityResponse(available,
                        available ? "Username is available" : "Username is already taken")
        );
    }

    /**
     * Check email availability
     */
    @GetMapping("/check-email/{email}")
    public ResponseEntity<?> checkEmail(@PathVariable String email) {
        log.debug("Checking email availability: {}", email);

        boolean available = authService.isEmailAvailable(email);

        return ResponseEntity.ok().body(
                new CheckAvailabilityResponse(available,
                        available ? "Email is available" : "Email is already registered")
        );
    }

    /**
     * Health check endpoint for authentication service
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body(
                new HealthResponse("Authentication service is running", LocalDateTime.now())
        );
    }

    // Inner classes for response DTOs
    private record CheckAvailabilityResponse(boolean available, String message) {}
    private record HealthResponse(String status, LocalDateTime timestamp) {}
}