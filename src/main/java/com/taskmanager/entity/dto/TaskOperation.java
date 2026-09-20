package com.taskmanager.entity.dto;

import com.taskmanager.utils.TaskOperationAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskOperation {
    private Long id;
    private Long taskId;
    private TaskOperationAction action;  // 正常情况为 COMPLETE
}
