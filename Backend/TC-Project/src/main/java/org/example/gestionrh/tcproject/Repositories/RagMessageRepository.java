package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.RagMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RagMessageRepository extends JpaRepository<RagMessage, Long> {
    List<RagMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Modifying
    @Query("DELETE FROM RagMessage rm WHERE rm.conversation.id IN (SELECT rc.id FROM RagConversation rc WHERE rc.user.id = :userId)")
    void deleteAllByConversationUserId(@Param("userId") Long userId);
}
