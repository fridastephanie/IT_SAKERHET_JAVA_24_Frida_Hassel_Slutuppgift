package se.gritacademy.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.gritacademy.models.UserInfo;
import se.gritacademy.service.AuthService;

@RestController
@RequestMapping("api/")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String email, @RequestParam String password) {
        try {
            ResponseEntity<String> validationResponse = authService.validateInputs(email, password);
            if (validationResponse != null) {
                return validationResponse;
            }
            authService.createUser(email, password);
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        } catch (Exception e) {
            return authService.handleException(e, "Error during registration");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String email, @RequestParam String password) {
        try {
            UserInfo user = authService.findUserByEmail(email);
            ResponseEntity<String> validationResponse = authService.validateUserStatus(user);
            if (validationResponse != null) {
                return validationResponse;
            }
            return authService.verifyPassword(password, user);
        } catch (RuntimeException e) {
            return authService.handleFailedLoginAttempt(email);
        } catch (Exception e) {
            return authService.handleException(e, "Error during login");
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<String> logout() {
        try {
            return authService.handleLogout();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during logout");
        }
    }
}