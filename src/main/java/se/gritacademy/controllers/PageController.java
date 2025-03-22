package se.gritacademy.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping({"/user"})
    public String showUserPage() {
        return "user";
    }

    @GetMapping({"/admin"})
    public String showAdminPage() {
        return "admin";
    }
}