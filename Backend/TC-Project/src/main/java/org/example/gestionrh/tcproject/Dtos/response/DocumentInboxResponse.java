package org.example.gestionrh.tcproject.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentInboxResponse {
    private Long id;
    private DocumentResponse document;
    private boolean isRead;
    private LocalDateTime readAt;
    private org.example.gestionrh.tcproject.Entities.ValidationStatus validationStatus;
    private UserResponse recipient;
}
