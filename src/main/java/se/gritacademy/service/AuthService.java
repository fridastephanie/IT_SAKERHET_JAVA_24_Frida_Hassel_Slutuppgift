package se.gritacademy.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.gritacademy.models.UserInfo;
import se.gritacademy.repositories.UserRepository;
import se.gritacademy.utils.HashingUtil;
import se.gritacademy.utils.JwtUtil;

import java.io.IOException;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Handles various types of exceptions and returns an appropriate response
     */
    public ResponseEntity<String> handleException(Exception e, String errorMessage) {
        if (e instanceof ExpiredJwtException) {
            logger.warn("JWT token has expired: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("JWT token has expired");
        } else if (e instanceof JwtException) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid JWT token");
        } else if (e instanceof EmptyResultDataAccessException) {
            logger.warn("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        } else if (e instanceof IOException) {
            logger.error("I/O error occurred: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("I/O error occurred: " + errorMessage);
        } else {
            logger.error("Unknown error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unknown error occurred: " + errorMessage);
        }
    }

    /**
     * Validates email and password before creating a user
     */
    public ResponseEntity<String> validateInputs(String email, String password) {
        String emailValidationError = validateEmailFormat(email);
        if (emailValidationError != null) {
            logger.warn("Failed registration attempt - Invalid email format: {}", email);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(emailValidationError);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            logger.warn("Failed registration attempt - Email already in use: {}", email);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use");
        }
        if (validatePasswordPolicy(password) != null) {
            logger.warn("Failed registration attempt - Password does not meet the policy for user: {}", email);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password does not meet the password policy");
        }
        return null;
    }

    /**
     * Validates the email format using regex
     */
    private String validateEmailFormat(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (!email.matches(emailRegex)) {
            return "Invalid email format";
        }
        return null;
    }

    /**
     * Validates the password against security policies, ensuring it meets length and complexity requirement
     */
    private String validatePasswordPolicy(String password) {
        if (password.length() < 12) {
            return "Password must be at least 12 characters long";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number";
        }
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            return "Password must contain at least one special character (like !, @, #)";
        }
        return null;
    }

    /**
     * Creates and saves a new user to the database
     */
    public void createUser(String email, String password) throws Exception {
        userRepository.save(new UserInfo(email, HashingUtil.hashPasswordWithPBKDF2(password), "user"));
        logger.info("New user registered: {}", email);
    }

    /**
     * Finds the user by email in database, throws a RuntimeException if email is not found
     */
    public UserInfo findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() ->
                new RuntimeException("User not found")
        );
    }

    /**
     * Verifies the password and generates a JWT token or returns an unauthorized response
     */
    public ResponseEntity<String> verifyPassword(String inputPassword, UserInfo user) throws Exception {
        if (HashingUtil.verifyPassword(inputPassword, user.getPassword())) {
            return generateJwtResponse(user);
        } else {
            logger.warn("Failed login attempt while verifying password for email: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }
    }

    /**
     * Generates a JWT token for the user
     */
    private ResponseEntity<String> generateJwtResponse(UserInfo user) {
        String token = JwtUtil.generateJwtToken(user);
        logger.info("Successful login: {}", user.getEmail());
        return ResponseEntity.ok(token);
    }

    /**
     * Checks if the user is blocked and returns a forbidden response if so
     */
    public ResponseEntity<String> validateUserStatus(UserInfo user) {
        if (user.isBlocked()) {
            logger.warn("Blocked login attempt for email: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your account is blocked");
        }
        return null;
    }

    /**
     * Handles a failed login attempt when the user is not found
     */
    public ResponseEntity<String> handleFailedLoginAttempt(String email) {
        logger.warn("Failed login attempt for email: {}", email);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
    }

    /**
     * Logs the user out and returns a success message
     */
    public ResponseEntity<String> handleLogout() {
        logger.info("User logged out");
        return ResponseEntity.ok("Logged out successfully");
    }
}
