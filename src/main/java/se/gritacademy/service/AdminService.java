package se.gritacademy.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.gritacademy.models.Message;
import se.gritacademy.models.UserInfo;
import se.gritacademy.repositories.MessageRepository;
import se.gritacademy.repositories.UserRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Autowired
    public AdminService(UserRepository userRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin(Claims claims) {
        String role = claims.get("role", String.class);
        return "admin".equals(role);
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
        } else if (e instanceof IOException) {
            logger.error("I/O error occurred: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("I/O error occurred: " + errorMessage);
        } else {
            logger.error("Unknown error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unknown error occurred: " + errorMessage);
        }
    }

    /**
     * Retrieves the user list and filters out the logged in administrator
     */
    public List<Map<String, Object>> getUsersForAdminResponse(String loggedInUserEmail) {
        logger.info("User list requested by admin: {}", loggedInUserEmail);
        List<UserInfo> users = userRepository.findAll();
        return users.stream()
                .filter(user -> !user.getEmail().equals(loggedInUserEmail))
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("email", user.getEmail());
                    userMap.put("blocked", user.isBlocked());
                    return userMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Handles blocking or unblocking users
     */
    public ResponseEntity<String> processUserBlocking(String loggedInUserEmail, String email, boolean block) {
        Optional<UserInfo> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            logger.info("Admin with email {} has failed to {} user with email {} because email does not exist",
                    loggedInUserEmail, block ? "blocked" : "unblocked", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        UserInfo user = userOpt.get();
        return updateUserBlockStatus(loggedInUserEmail, email, block, user);
    }

    /**
     * Updates the user's blocking state (blocked/unblocked)
     */
    private ResponseEntity<String> updateUserBlockStatus(String loggedInUserEmail, String email, boolean block, UserInfo user) {
        user.setBlocked(block);
        userRepository.save(user);
        logger.info("Admin with email {} has {} user with email {}", loggedInUserEmail, block ? "blocked" : "unblocked", email);
        return ResponseEntity.ok("User " + (block ? "blocked" : "unblocked") + " successfully");
    }

    /**
     * Builds a list of all messages in the database for the admin
     */
    public List<Map<String, Object>> buildMessageList(List<Message> messages, String loggedInUserEmail) {
        logger.info("Messages list requested by admin: {}", loggedInUserEmail);
        List<Map<String, Object>> responseMessages = new ArrayList<>();
        for (Message message : messages) {
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", message.getId());
            messageData.put("sender", message.getSender());
            messageData.put("recipient", message.getReceiver());
            messageData.put("message", message.getEncryptedMessage());
            messageData.put("date", message.getDate());
            responseMessages.add(messageData);
        }
        return responseMessages;
    }

    /**
     * Deletes a message by ID and returns the result
     */
    public ResponseEntity<String> deleteMessageById(Long messageId, String loggedInUserEmail) {
        if (messageRepository.existsById(messageId)) {
            messageRepository.deleteById(messageId);
            logger.info("Message with id {} deleted by admin: {}", messageId, loggedInUserEmail);
            return ResponseEntity.ok("Message deleted successfully");
        } else {
            logger.info("Admin {} failed to delete message with id: {}", loggedInUserEmail, messageId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Message not found");
        }
    }

    /**
     * Retrieves the log file from the server, reads the contents, and checks if the file exists
     */
    public byte[] getLogFileContent(Claims claims) throws IOException {
        File logFile = new File("logs/application.log");
        if (!logFile.exists()) {
            logger.info("Admin {} failed to download logfile because it was not found", claims.getSubject());
            return null;
        }
        byte[] fileContent = Files.readAllBytes(logFile.toPath());
        logger.info("Logfile successfully downloaded by admin: {}", claims.getSubject());
        return fileContent;
    }

    /**
     * Prepare the log file response for download
     */
    public ResponseEntity<byte[]> prepareLogFileResponse(byte[] fileContent) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=application.log")
                    .body(fileContent);
    }
}