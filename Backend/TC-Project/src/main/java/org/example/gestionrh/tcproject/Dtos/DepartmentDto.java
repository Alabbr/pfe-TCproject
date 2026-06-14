package org.example.gestionrh.tcproject.Dtos;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDto {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private Boolean isActive;
}
