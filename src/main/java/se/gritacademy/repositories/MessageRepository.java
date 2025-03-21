package se.gritacademy.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.gritacademy.models.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByReceiverOrderByDateDesc(String receiver);
}
