package com.taskhive.dto;

import com.taskhive.model.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskDto {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private TaskPriority priority;

    private String assigneeEmail;
}
