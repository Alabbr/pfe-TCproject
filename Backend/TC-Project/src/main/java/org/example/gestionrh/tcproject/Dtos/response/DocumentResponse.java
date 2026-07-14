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
public class DocumentResponse {
    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private String fileType;
    private String originalFileName;
    private Long fileSize;
    private boolean isPublic;
    private LocalDateTime uploadDate;
    private UserResponse uploader;
}
