package se.gritacademy.controllers;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.gritacademy.repositories.MessageRepository;
import se.gritacademy.repositories.UserRepository;
import se.gritacademy.service.AdminService;
import se.gritacademy.utils.JwtUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@RestController
@RequestMapping("api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final MessageRepository messageRepository;

    @Autowired
    public AdminController(AdminService adminService, MessageRepository MessageRepository) {
        this.adminService = adminService;
        this.messageRepository = MessageRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<?> adminGetUsers(@RequestHeader("Authorization") String token) {
        try {
        Claims claims = JwtUtil.parseJwtToken(token.replace("Bearer ", ""));
        if (!adminService.isAdmin(claims)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }
        List<Map<String, Object>> userResponse = adminService.getUsersForAdminResponse(claims.getSubject());
        return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            return adminService.handleException(e,"Error while getting users for admin");
        }
    }

    @PostMapping("/block")
    public ResponseEntity<String> adminBlockUser(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> payload) {
        try {
            Claims claims = JwtUtil.parseJwtToken(token.replace("Bearer ", ""));
            if (!adminService.isAdmin(claims)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
            }
            String email = (String) payload.get("email");
            boolean block = (boolean) payload.get("block");
            return adminService.processUserBlocking(claims.getSubject(), email, block);
        } catch (Exception e) {
            return adminService.handleException(e, "Error while blocking user");
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<?> getMessagesForAdmin(@RequestHeader("Authorization") String token) {
        try {
            Claims claims = JwtUtil.parseJwtToken(token.replace("Bearer ", ""));
            if (!adminService.isAdmin(claims)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
            }
            List<Map<String, Object>> messageList = adminService.buildMessageList(messageRepository.findAll(), claims.getSubject());
            return ResponseEntity.ok(messageList);
        } catch (Exception e) {
            return adminService.handleException(e, "Error occurred while fetching messages");
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<String> deleteMessage(@RequestHeader("Authorization") String token, @RequestBody Map<String, Long> requestBody) {
        try {
            Claims claims = JwtUtil.parseJwtToken(token.replace("Bearer ", ""));
            Long messageId = requestBody.get("messageId");
            if (!adminService.isAdmin(claims)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
            }
            return adminService.deleteMessageById(messageId, claims.getSubject());
        } catch (Exception e) {
            return adminService.handleException(e, "Error while deleting the message");
        }
    }

    @GetMapping("/log")
    public ResponseEntity<?> getLogFile(@RequestHeader("Authorization") String token) {
        try {
            Claims claims = JwtUtil.parseJwtToken(token.replace("Bearer ", ""));
            if (!adminService.isAdmin(claims)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
            }
            byte[] fileContent = adminService.getLogFileContent(claims);
            if (fileContent == null) {
                return ResponseEntity.status(404).body("Log file not found");
            }
            return adminService.prepareLogFileResponse(fileContent);
        } catch (Exception e) {
            return adminService.handleException(e, "Error while downloading log file");
        }
    }
}