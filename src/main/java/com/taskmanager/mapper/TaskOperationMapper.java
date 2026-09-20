package com.taskmanager.mapper;

import com.taskmanager.entity.dto.TaskOperation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

/**
 * 与 task_operation 表交互的 Mapper
 */
public interface TaskOperationMapper {

    /**
     * 根据 task_record 的 id 插入一条完成任务的记录
     */
    @Insert("insert into task_operation (task_id,action) values (#{operation.taskId},#{operation.action})")
    @Options(useGeneratedKeys = true, keyProperty = "operation.id")
    int insertTaskOperation(@Param("operation") TaskOperation operation);

}
