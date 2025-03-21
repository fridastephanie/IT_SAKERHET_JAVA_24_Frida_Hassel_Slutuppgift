package se.gritacademy.models;

import jakarta.persistence.*;

@Entity
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private String role = "user";
    @Column(nullable = false)
    private boolean isBlocked = false;

    public UserInfo() {
    }

    public UserInfo(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }
    public String getRole() {
        return role;
    }
    public boolean isBlocked() {
        return isBlocked;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }
}
