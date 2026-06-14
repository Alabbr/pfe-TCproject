package org.example.gestionrh.tcproject.Services;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.response.SuperAdminDashboardStatsDto;
import org.example.gestionrh.tcproject.Entities.Department;
import org.example.gestionrh.tcproject.Entities.RoleName;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.DepartmentRepository;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminDashboardService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public SuperAdminDashboardStatsDto getDashboardStats() {
        List<User> allUsers = userRepository.findAll();
        List<Department> allDepartments = departmentRepository.findAll();

        long totalUsers = allUsers.size();
        long totalDepartments = allDepartments.size();

        // Calculate active users today
        LocalDate today = LocalDate.now();
        long activeUsersToday = allUsers.stream()
                .filter(u -> u.getLastLogin() != null && u.getLastLogin().toLocalDate().isEqual(today))
                .count();

        List<User> pendingUsersList = userRepository.findByIsActiveFalse();
        long pendingUsersCount = pendingUsersList.size();

        // Department distribution
        Map<String, Long> deptCount = allUsers.stream()
                .filter(u -> u.getDepartment() != null)
                .collect(Collectors.groupingBy(u -> u.getDepartment().getName(), Collectors.counting()));
        
        List<SuperAdminDashboardStatsDto.ChartData> deptChart = deptCount.entrySet().stream()
                .map(e -> new SuperAdminDashboardStatsDto.ChartData(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // Role distribution
        Map<RoleName, Long> roleCount = allUsers.stream()
                .filter(u -> u.getRole() != null)
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
        
        List<SuperAdminDashboardStatsDto.ChartData> roleChart = roleCount.entrySet().stream()
                .map(e -> new SuperAdminDashboardStatsDto.ChartData(e.getKey().name(), e.getValue()))
                .collect(Collectors.toList());

        // Top users (recent active)
        List<User> topActiveUsers = userRepository.findTop5ByIsActiveTrueOrderByLastLoginDesc();
        List<SuperAdminDashboardStatsDto.TopUserDto> topUsers = topActiveUsers.stream().map(u -> {
            // Fake activity score based on how recent last login is
            int score = (u.getLastLogin() != null && u.getLastLogin().toLocalDate().isEqual(today)) ? 95 : 75;
            return SuperAdminDashboardStatsDto.TopUserDto.builder()
                    .id(u.getId())
                    .fullName(u.getFullName())
                    .role(u.getRole().name())
                    .departmentName(u.getDepartment() != null ? u.getDepartment().getName() : "N/A")
                    .profilePictureUrl(u.getProfilePictureUrl())
                    .lastLogin(u.getLastLogin() != null ? u.getLastLogin().toString() : "N/A")
                    .activityScore(score)
                    .build();
        }).collect(Collectors.toList());

        // Pending users mapped
        List<SuperAdminDashboardStatsDto.PendingUserDto> pendingUsers = pendingUsersList.stream().map(u -> 
                SuperAdminDashboardStatsDto.PendingUserDto.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .role(u.getRole() != null ? u.getRole().name() : "N/A")
                        .departmentName(u.getDepartment() != null ? u.getDepartment().getName() : "N/A")
                        .requestedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "N/A")
                        .build()
        ).collect(Collectors.toList());

        return SuperAdminDashboardStatsDto.builder()
                .totalUsers(totalUsers)
                .totalDepartments(totalDepartments)
                .activeUsersToday(activeUsersToday)
                .pendingUsersCount(pendingUsersCount)
                .departmentDistribution(deptChart)
                .roleDistribution(roleChart)
                .topUsers(topUsers)
                .pendingUsers(pendingUsers)
                .build();
    }
}
