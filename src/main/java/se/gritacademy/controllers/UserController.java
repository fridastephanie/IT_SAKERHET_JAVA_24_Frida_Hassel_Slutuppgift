package se.gritacademy.controllers;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.gritacademy.models.Message;
import se.gritacademy.repositories.MessageRepository;
import se.gritacademy.repositories.UserRepository;
import se.gritacademy.service.UserService;
import se.gritacademy.utils.JwtUtil;

import java.util.*;

@RestController
@RequestMapping("api/user")
@CrossOrigin(origins = "*")
public class UserController {

    private final MessageRepository messageRepository;
    private final UserService userService;

    @Autowired
    public UserController(MessageRepository messageRepository, UserService userService) {
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestHeader("Authorization") String token) {
        try {
            Claims claims = JwtUtil.parseJwtToken(token.replace("Bearer ", ""));
            List<Map<String, String>> emails = userService.getUserList(claims.getSubject());
            return ResponseEntity.ok(emails);
        } catch (Exception e) {
            return userService.handleException(e, "Error while retrieving user list");
        }
    }

    @PostMapping("/messages")
    public ResponseEntity<String> sendMessage(@RequestHeader("Authorization") String token, @RequestParam String receiver, @RequestParam String message) {
        try {
            Claims claims = JwtUtil.parseJwtToken(token.replace("Bearer ", ""));
            ResponseEntity<String> messageValidation = userService.validateMessage(message);
            if (messageValidation != null) {
                return messageValidation;
            }
            ResponseEntity<String> receiverValidation = userService.validateReceiver(receiver);
            if (receiverValidation != null) {
                return receiverValidation;
            }
            return userService.encryptAndSaveMessage(claims.getSubject(), receiver, message);
        } catch (Exception e) {
            return userService.handleException(e, "Error while sending message");
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<?> getMessages(@RequestHeader("Authorization") String token) {
        try {
            Claims claims = JwtUtil.parseJwtToken(token.replace("Bearer ", ""));
            List<Message> messages = messageRepository.findByReceiverOrderByDateDesc(claims.getSubject());
            List<Map<String, Object>> messageList = userService.decryptAllMessages(messages, claims.getSubject());
            return ResponseEntity.ok(messageList);
        } catch (Exception e) {
            return userService.handleException(e, "Error while fetching messages");
        }
    }
}