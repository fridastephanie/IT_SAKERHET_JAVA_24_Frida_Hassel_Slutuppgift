package se.gritacademy.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import se.gritacademy.models.Message;
import se.gritacademy.models.UserInfo;
import se.gritacademy.repositories.MessageRepository;
import se.gritacademy.repositories.UserRepository;
import se.gritacademy.utils.CryptoUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    @Value("${crypto.key}")
    private String cryptoKey;

    @Autowired
    public UserService(UserRepository userRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
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
     * Retrieves a list of user emails excluding the logged-in user
     */
    public List<Map<String, String>> getUserList(String loggedInUserEmail) {
        logger.info("User list requested by: {}", loggedInUserEmail);
        List<UserInfo> users = userRepository.findAll();

        return users.stream()
                .filter(user -> !user.getEmail().equals(loggedInUserEmail))
                .map(user -> {
                    Map<String, String> userMap = new HashMap<>();
                    userMap.put("email", user.getEmail());
                    userMap.put("role", user.getRole());
                    return userMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Validates if message is empty
     */
    public ResponseEntity<String> validateMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message cannot be empty");
        }
        return null;
    }

    /**
     * Validates if the receiver exists in the database by checking their email
     */
    public ResponseEntity<String> validateReceiver(String receiver) {
        Optional<UserInfo> receivingUser = userRepository.findByEmail(receiver);
        if (receivingUser.isEmpty()) {
            logger.warn("Failed to send message - Receiver does not exist: {}", receiver);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Receiver does not exist");
        }
        return null;
    }

    /**
     * Encrypts the message and saves it to the database
     */
    public ResponseEntity<String> encryptAndSaveMessage(String senderEmail, String receiver, String message) throws Exception {
        String hashedSecretKey = CryptoUtil.hashKey(cryptoKey);
        String encryptedMessageWithComponents = CryptoUtil.encryptMessage(message, hashedSecretKey);
        String[] encryptedData = encryptedMessageWithComponents.split(":");
        if (encryptedData.length != 3) {
            logger.error("Encryption failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Encryption failed");
        }
        saveMessage(senderEmail, receiver, encryptedData);
        logger.info("Message sent successfully by: {}", senderEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body("Message sent successfully");
    }

    /**
     * Saves a message to the database, using the provided encrypted data components
     */
    private void saveMessage(String senderEmail, String receiver, String[] encryptedData) {
        String encryptedMessage = encryptedData[0];
        String nonce = encryptedData[1];
        String authTag = encryptedData[2];
        Message newMessage = new Message(
                senderEmail,
                receiver,
                encryptedMessage,
                nonce,
                authTag,
                new Date()
        );
        messageRepository.save(newMessage);
        logger.info("User {} sent a message to {}", senderEmail, receiver);
    }

    /**
     * Decrypts all messages in the list and returns a list of message data maps
     */
    public List<Map<String, Object>> decryptAllMessages(List<Message> messages, String userEmail) throws Exception {
        logger.info("Message list requested by: {}", userEmail);
        String hashedSecretKey = CryptoUtil.hashKey(cryptoKey);
        List<Map<String, Object>> responseMessages = new ArrayList<>();
        for (Message message : messages) {
            String decryptedMessage = decryptSingleMessage(message, hashedSecretKey);
            Map<String, Object> msgData = new HashMap<>();
            msgData.put("sender", message.getSender());
            msgData.put("receiver", message.getReceiver());
            msgData.put("message", decryptedMessage);
            msgData.put("date", message.getDate());
            responseMessages.add(msgData);
        }
        return responseMessages;
    }

    /**
     * Decrypts the content of a single message using the provided secret key
     */
    private String decryptSingleMessage(Message message, String hashedSecretKey) throws Exception {
        String encryptedData = message.getEncryptedMessage() + ":" + message.getNonce() + ":" + message.getAuthTag();
        return CryptoUtil.decryptMessage(encryptedData, hashedSecretKey);
    }
}