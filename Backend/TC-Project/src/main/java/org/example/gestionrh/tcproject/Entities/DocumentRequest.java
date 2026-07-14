package org.example.gestionrh.tcproject.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_department_id", nullable = false)
    private Department targetDepartment;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "document_type")
    private String documentType; // ATTESTATION_TRAVAIL, FICHE_PAIE, AUTRE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_to_id")
    private User delegatedTo;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ASSIGNED, FULFILLED, REJECTED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
