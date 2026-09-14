package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.RagConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RagConversationRepository extends JpaRepository<RagConversation, Long> {
    List<RagConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Modifying
    @Query("DELETE FROM RagConversation rc WHERE rc.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
