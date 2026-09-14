package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    List<MessageReaction> findByMessageId(Long messageId);

    @Modifying
    @Query("DELETE FROM MessageReaction r WHERE r.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM MessageReaction r WHERE r.message.id IN (SELECT m.id FROM ChatMessage m WHERE m.sender.id = :userId OR m.receiver.id = :userId)")
    void deleteAllByMessageUserId(@Param("userId") Long userId);
}
