package se.gritacademy;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import se.gritacademy.models.UserInfo;
import se.gritacademy.repositories.UserRepository;
import se.gritacademy.service.UserService;
import se.gritacademy.utils.HashingUtil;

@SpringBootApplication
public class MessageServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessageServerApplication.class, args);
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    @PostConstruct
    public void init() throws Exception {
        UserInfo userAdmin = new UserInfo("admin@admin.se", HashingUtil.hashPasswordWithPBKDF2("HiAdmin123!!!"), "admin");
        userRepository.save(userAdmin);
        UserInfo user1 = new UserInfo("user1@user.se", HashingUtil.hashPasswordWithPBKDF2("HiUser123!!!"), "user");
        userRepository.save(user1);
        UserInfo user2 = new UserInfo("user2@user.se", HashingUtil.hashPasswordWithPBKDF2("HiUser123!!!"), "user");
        userRepository.save(user2);

        userService.encryptAndSaveMessage("user1@user.se", "user2@user.se", "Hello!");
        userService.encryptAndSaveMessage("user2@user.se", "user1@user.se", "Hi :)");
        userService.encryptAndSaveMessage("user1@user.se", "user2@user.se", "How are you?");
        userService.encryptAndSaveMessage("user2@user.se", "user1@user.se", "Fine, how are you?");
        userService.encryptAndSaveMessage("user1@user.se", "user2@user.se", "Great!");
    }
}



