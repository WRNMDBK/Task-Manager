package com.taskmanager.task.domain;

import com.taskmanager.task.domain.enums.TaskOperationAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskOperation {
    private Long id;
    private Long taskId;
    private TaskOperationAction action;  // 正常情况为 COMPLETE
}
