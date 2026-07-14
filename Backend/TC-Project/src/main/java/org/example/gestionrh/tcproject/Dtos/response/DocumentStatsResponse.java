package org.example.gestionrh.tcproject.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatsResponse {
    private long totalSent;
    private long totalReceived;
    private long unreadCount;
    private long publicDocsCount;
    private long pendingCount; // Existing: for document validation
    private long assignedTasksCount; // New: for HR employees (ASSIGNED document requests)
    private long pendingRequestsCount; // New: for Chef (PENDING document requests)
}
