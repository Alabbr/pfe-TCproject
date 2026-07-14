package org.example.gestionrh.tcproject.Services;

import org.example.gestionrh.tcproject.Entities.Project;
import org.example.gestionrh.tcproject.Entities.ProjectDocument;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.ProjectDocumentRepository;
import org.example.gestionrh.tcproject.Repositories.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectDocumentService {
    
    private final ProjectDocumentRepository documentRepository;
    private final ProjectRepository projectRepository;

    public ProjectDocumentService(ProjectDocumentRepository documentRepository, ProjectRepository projectRepository) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
    }

    public ProjectDocument uploadDocument(Long projectId, MultipartFile file, User user) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        
        ProjectDocument doc = new ProjectDocument();
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(file.getContentType());
        doc.setData(file.getBytes());
        doc.setProject(project);
        doc.setUploadedBy(user);
        
        return documentRepository.save(doc);
    }
    
    public List<Map<String, Object>> getProjectDocuments(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        return documentRepository.findByProject(project).stream().map(doc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", doc.getId());
            map.put("fileName", doc.getFileName());
            map.put("fileType", doc.getFileType());
            map.put("uploadedAt", doc.getUploadedAt());
            
            Map<String, Object> uploader = new HashMap<>();
            uploader.put("id", doc.getUploadedBy().getId());
            uploader.put("firstName", doc.getUploadedBy().getFirstName());
            uploader.put("lastName", doc.getUploadedBy().getLastName());
            map.put("uploadedBy", uploader);
            
            return map;
        }).collect(Collectors.toList());
    }
    
    public ProjectDocument getDocument(Long id) {
        return documentRepository.findById(id).orElseThrow(() -> new RuntimeException("Document not found"));
    }
    
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }
}
