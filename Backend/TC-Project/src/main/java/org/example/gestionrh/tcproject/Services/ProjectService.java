package org.example.gestionrh.tcproject.Services;

import org.example.gestionrh.tcproject.Entities.Project;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.ProjectRepository;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public Project createProject(Map<String, Object> data, User creator) {
        Project project = new Project();
        project.setTitle((String) data.get("title"));
        project.setDescription((String) data.get("description"));
        if (data.get("deadline") != null) {
            project.setDeadline(LocalDate.parse(data.get("deadline").toString()));
        }
        if (data.get("startDate") != null) {
            project.setStartDate(LocalDate.parse(data.get("startDate").toString()));
        }
        project.setCreatedBy(creator);
        
        List<?> rawMemberIds = (List<?>) data.get("memberIds");
        if (rawMemberIds != null && !rawMemberIds.isEmpty()) {
            List<Long> memberIds = rawMemberIds.stream()
                .map(id -> Long.parseLong(id.toString()))
                .collect(Collectors.toList());
            List<User> members = userRepository.findAllById(memberIds);
            project.setMembers(members);
        }
        
        return projectRepository.save(project);
    }

    public List<Map<String, Object>> getAllProjectsForUser(User user) {
        List<Project> allProjects;
        // If super admin or general director, maybe see all. But let's assume all users can see projects they created or are members of.
        // For simplicity, let's just return all projects for now if they are admin, else only their projects.
        boolean isAdmin = user.getRole().name().equals("SUPER_ADMIN") || user.getRole().name().equals("DIRECTEUR_GENERAL");
        
        if (isAdmin) {
            allProjects = projectRepository.findAll();
        } else {
            List<Project> created = projectRepository.findByCreatedBy(user);
            List<Project> memberOf = projectRepository.findByMembersContaining(user);
            created.removeAll(memberOf);
            created.addAll(memberOf);
            allProjects = created;
        }

        return allProjects.stream().map(this::mapToDTO).collect(Collectors.toList());
    }
    
    public Map<String, Object> getProjectById(Long id) {
        Project p = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Projet introuvable"));
        return mapToDTO(p);
    }

    public Project updateProject(Long id, Map<String, Object> data, User user) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Projet introuvable"));
        
        boolean isCreator = project.getCreatedBy().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("SUPER_ADMIN") || user.getRole().name().equals("DIRECTEUR_GENERAL");
        
        if (!isCreator && !isAdmin) {
            throw new RuntimeException("Non autorisé à modifier ce projet");
        }

        if (data.containsKey("title")) project.setTitle((String) data.get("title"));
        if (data.containsKey("description")) project.setDescription((String) data.get("description"));
        if (data.get("deadline") != null) project.setDeadline(LocalDate.parse(data.get("deadline").toString()));
        
        List<?> rawMemberIds = (List<?>) data.get("memberIds");
        if (rawMemberIds != null) {
            List<Long> memberIds = rawMemberIds.stream()
                .map(idStr -> Long.parseLong(idStr.toString()))
                .collect(Collectors.toList());
            List<User> members = userRepository.findAllById(memberIds);
            project.setMembers(members);
        }
        
        return projectRepository.save(project);
    }

    public void deleteProject(Long id, User user) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Projet introuvable"));
        
        boolean isCreator = project.getCreatedBy().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("SUPER_ADMIN") || user.getRole().name().equals("DIRECTEUR_GENERAL");
        
        if (!isCreator && !isAdmin) {
            throw new RuntimeException("Non autorisé à supprimer ce projet");
        }
        
        projectRepository.delete(project);
    }

    private Map<String, Object> mapToDTO(Project p) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", p.getId());
        dto.put("title", p.getTitle());
        dto.put("description", p.getDescription());
        dto.put("status", p.getStatus());
        dto.put("startDate", p.getStartDate());
        dto.put("deadline", p.getDeadline());
        
        Map<String, Object> creator = new HashMap<>();
        creator.put("id", p.getCreatedBy().getId());
        creator.put("firstName", p.getCreatedBy().getFirstName());
        creator.put("lastName", p.getCreatedBy().getLastName());
        dto.put("createdBy", creator);
        
        List<Map<String, Object>> members = new ArrayList<>();
        if (p.getMembers() != null) {
            for (User u : p.getMembers()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("firstName", u.getFirstName());
                m.put("lastName", u.getLastName());
                m.put("email", u.getEmail());
                members.add(m);
            }
        }
        dto.put("members", members);
        
        return dto;
    }
}
