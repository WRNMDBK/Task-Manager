package com.taskmanager.service;

import com.taskmanager.entity.dto.Task;
import com.taskmanager.entity.vo.CreateTaskRequest;

import java.util.List;
import java.util.Map;

public interface TaskService {

    /**
     * 创建任务并插入 task_record 表中
     * @param taskRequest task请求参数容器对象
     * @return 成功插入后 Task 的 ID
     */
    Long createTask(CreateTaskRequest taskRequest);

    /**
     * 根据 id 查询对应的 Task 信息
     * @param id task_record 表的主键 id
     * @return Task 对象
     */
    Task selectTaskById(Long id);

    /**
     * 根据 status 返回对应的 task 列表并以 id 升序排序
     * @param status 状态（PENDING/DONE，若为空则全选）
     * @return task 列表，没查到则为空
     */
    List<Task> getTaskList(String status);

    /**
     * 根据 id 设置指定 task_record 记录的状态为 DONE，并在 task_operation 插入操作记录
     * 若其中任意一步失败则全部回滚
     * @param id task_record 的主键 id
     */
    void completeTask(Long id);

    /**
     * 根据 id 删除任务
     */
    int deleteTask(Long id);
}
