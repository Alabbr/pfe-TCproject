package org.example.gestionrh.tcproject.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_department_id", nullable = false)
    private Department targetDepartment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "new_job_position_id")
    private JobPosition newJobPosition;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy; // Chef de département

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationStatus status; // PENDING, APPROVED, REJECTED

    @Column(name = "request_date", nullable = false, updatable = false)
    private LocalDateTime requestDate;

    @Column(name = "decision_date")
    private LocalDateTime decisionDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "decided_by_id")
    private User decidedBy; // Super Admin ou DG

    private String comment;

    @PrePersist
    protected void onCreate() {
        this.requestDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = ValidationStatus.PENDING;
        }
    }
}
