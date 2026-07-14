package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.RagMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RagMessageRepository extends JpaRepository<RagMessage, Long> {
    List<RagMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
