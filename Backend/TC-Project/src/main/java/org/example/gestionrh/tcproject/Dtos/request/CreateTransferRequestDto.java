package org.example.gestionrh.tcproject.Dtos.request;

import lombok.Data;

@Data
public class CreateTransferRequestDto {
    private Long employeeId;
    private Long targetDepartmentId;
    private Long newJobPositionId; // Optional
    private String comment;
}
