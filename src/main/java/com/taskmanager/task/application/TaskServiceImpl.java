package com.taskmanager.task.application;

import com.taskmanager.task.domain.Task;
import com.taskmanager.task.domain.TaskOperation;
import com.taskmanager.task.adapter.dto.CreateTaskRequest;
import com.taskmanager.shared.exception.BusinessException;
import com.taskmanager.task.infrastructure.TaskMapper;
import com.taskmanager.task.infrastructure.TaskOperationMapper;
import com.taskmanager.shared.web.ResultCode;
import com.taskmanager.task.domain.enums.TaskOperationAction;
import com.taskmanager.task.domain.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final TaskOperationMapper taskOperationMapper;

    /**
     * 创建任务并插入 task_record 表中
     * @param taskRequest task请求参数容器对象
     * @return 成功插入后 task_record 的 id
     */
    public Long createTask(CreateTaskRequest taskRequest) {
        String title = taskRequest.getTitle();
        String description = taskRequest.getDescription();
        LocalDateTime createdTime = LocalDateTime.now();
        Task task = Task.builder().title(title).description(description).status(TaskStatus.PENDING).createdTime(createdTime).build();
        int result = taskMapper.insertTask(task);
        if (result == 0) throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR,"数据库内部错误");
        return task.getId();
    }

    /**
     * 根据 id 查询对应的 task_record 信息
     * @param id task_record 表的主键 id
     * @return Task 对象
     */
    public Task selectTaskById(Long id) {
        Task task = taskMapper.selectTaskById(id);
        if (task == null) throw new BusinessException(ResultCode.NOT_EXIST);
        return task;
    }

    /**
     * 根据 status 返回对应的 task——record 列表并以 id 升序排序
     * @param status 状态（PENDING/DONE，若为空则全选）
     * @return task_record 列表，没查到则为空
     */
    public List<Task> getTaskList(String status) {
        return taskMapper.getListByStatus(status);
    }

    /**
     * 根据 id 设置指定 task_record 记录的状态为 DONE，并在 task_operation 插入操作记录
     * 若其中任意一步失败则全部回滚
     * @param id task_record 的主键 id
     */
    @Transactional
    public void completeTask(Long id) {
        TaskOperation parameter = TaskOperation.builder().taskId(id).action(TaskOperationAction.COMPLETE).build();
        Task task = taskMapper.selectTaskById(id);
        if (task == null) throw new BusinessException(ResultCode.NOT_EXIST);
        int taskResult = taskMapper.setStatusAsDONE(id);
        if (taskResult == 0) throw new BusinessException(ResultCode.CONFLICT,"任务不可重复完成");
        int operationResult = taskOperationMapper.insertTaskOperation(parameter);
        if (operationResult == 0) throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR,"数据库内部错误");
    }

    /**
     * 根据 id 删除任务
     */
    public int deleteTask(Long id) {
        int rows = taskMapper.deleteTaskById(id);
        if (rows == 0) throw new BusinessException(ResultCode.NOT_EXIST);
        return rows;
    }

}
