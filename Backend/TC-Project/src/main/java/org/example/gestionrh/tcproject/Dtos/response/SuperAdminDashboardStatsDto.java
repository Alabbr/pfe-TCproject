package org.example.gestionrh.tcproject.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminDashboardStatsDto {
    private long totalUsers;
    private long totalDepartments;
    private long activeUsersToday;
    private long pendingUsersCount;

    private List<ChartData> departmentDistribution;
    private List<ChartData> roleDistribution;

    private List<TopUserDto> topUsers;
    private List<PendingUserDto> pendingUsers;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChartData {
        private String name;
        private long value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TopUserDto {
        private Long id;
        private String fullName;
        private String role;
        private String departmentName;
        private String profilePictureUrl;
        private String lastLogin;
        private int activityScore; // e.g. 95 for 95%
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PendingUserDto {
        private Long id;
        private String fullName;
        private String email;
        private String role;
        private String departmentName;
        private String requestedAt;
    }
}
