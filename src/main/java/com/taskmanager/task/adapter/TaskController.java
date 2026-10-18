package com.taskmanager.task.adapter;

import com.taskmanager.rateLimit.adpter.annotation.ipRateLimit;
import com.taskmanager.rateLimit.adpter.annotation.uidRateLimit;
import com.taskmanager.task.domain.Task;
import com.taskmanager.task.adapter.dto.CreateTaskRequest;
import com.taskmanager.task.application.TaskService;
import com.taskmanager.shared.web.Result;
import com.taskmanager.rateLimit.adpter.annotation.enums.IpLimitLevel;
import com.taskmanager.rateLimit.adpter.annotation.enums.RequestType;
import com.taskmanager.rateLimit.adpter.annotation.enums.UidLimitLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 创建任务
     * @param taskRequest 任务参数容器：标题（不能为空）、描述
     * @return 新增 task_record 的主键 ID
     */
    @PostMapping
    @ipRateLimit(type = RequestType.CREATE_TASK, level = IpLimitLevel.NORMAL)
    @uidRateLimit(type = RequestType.CREATE_TASK, level = UidLimitLevel.NORMAL)
    public Result<Long> createTask(@Valid @RequestBody CreateTaskRequest taskRequest) {
        Long result = taskService.createTask(taskRequest);
        return Result.success(result);
    }

    /**
     * 根据 ID 查询指定的 task_record 记录
     * @param id task_record 的主键 ID
     * @return Task 对象
     */
    @GetMapping("/{id}")
    @ipRateLimit(type = RequestType.GET_TASK_MESSAGE, level = IpLimitLevel.LOOSE)
    @uidRateLimit(type = RequestType.GET_TASK_MESSAGE, level = UidLimitLevel.LOOSE)
    public Result<Task> getTaskMessage(@PathVariable Long id) {
        Task task = taskService.selectTaskById(id);
        return Result.success(task);
    }

    /**
     * 根据 status 查询 task_record 列表，并以主键 ID 升序排序
     * 若 status 为空则查全部
     * @param status 状态
     * @return Task 列表
     */
    @GetMapping
    @ipRateLimit(type = RequestType.GET_TASK_LIST, level = IpLimitLevel.LOOSE)
    @uidRateLimit(type = RequestType.GET_TASK_LIST, level = UidLimitLevel.LOOSE)
    public Result<List<Task>> getTaskList(@Pattern(regexp = "^(PENDING|DONE)?$", message = "参数只能是PENDING或DONE") @RequestParam(value = "status", required = false) String status) {
        List<Task> taskList = taskService.getTaskList(status);
        return Result.success(taskList);
    }

    /**
     * 完成任务并增加操作记录
     * 若状态冲突则失败
     * @param id task_record 的主键 ID
     * @return 操作成功响应
     */
    @PostMapping("/{id}/complete")
    @ipRateLimit(type = RequestType.COMPLETE_TASK, level = IpLimitLevel.NORMAL)
    @uidRateLimit(type = RequestType.COMPLETE_TASK, level = UidLimitLevel.NORMAL)
    public Result<Void> completeTask(@PathVariable Long id) {
        taskService.completeTask(id);
        return Result.success();
    }

    /**
     * 删除一条任务
     * @param id task_record 的主键 ID
     * @return 操作成功响应
     */
    @DeleteMapping("/{id}")
    @ipRateLimit(type = RequestType.DELETE_TASK, level = IpLimitLevel.NORMAL)
    @uidRateLimit(type = RequestType.DELETE_TASK, level = UidLimitLevel.NORMAL)
    public Result<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return Result.success();
    }

}
