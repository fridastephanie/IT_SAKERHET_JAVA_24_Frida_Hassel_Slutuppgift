package se.gritacademy.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.gritacademy.models.UserInfo;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserInfo, Long> {

    Optional<UserInfo> findByEmail(String email);
}
