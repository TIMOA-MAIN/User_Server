package TIMOA.userserver.repository;

import TIMOA.userserver.entity.Chat_messages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface Chat_repository extends JpaRepository<Chat_messages, Long> {
    List<Chat_messages> findBySenderId(String senderId);

    @Modifying
    @Query("UPDATE Chat_messages cm SET cm.senderId = '[삭제된 사용자]' WHERE cm.senderId = :userId")
    void anonymizeMessagesBySenderId(@Param("userId") String userId);
}
