package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    List<MessageReaction> findByMessageId(Long messageId);
}
