package com.example.server1.entity;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class TaskDto {
    private String title;
    private String description;
    private String assignedUser;
    private String importance;
    private String deadline;
    private String status;
}
