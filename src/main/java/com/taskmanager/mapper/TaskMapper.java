package com.taskmanager.mapper;

import com.taskmanager.entity.dto.Task;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 与 task_record 表交互的 Mapper
 */
@Mapper
public interface TaskMapper {

    /**
     * 根据 id 查询一条完整的 Task 信息
     */
    @Select("select * from task_record where id = #{id}")
    @Results(id = "taskMap",value = {
            @Result(property = "id",column = "id"),
            @Result(property = "title",column = "title"),
            @Result(property = "description",column = "description"),
            @Result(property = "status",column = "status"),
            @Result(property = "createdTime",column = "created_time")
    })
    Task selectTaskById(@Param("id") Long id);

    /**
     * 插入一条 task 记录并返回 id
     */
    // 开启 useGeneratedKeys 后 insert 会将生成的 id 存进之前作为参数的 task 中，可以直接task.getId()拿
    @Insert("insert into task_record (title,description,status,created_time) values (#{task.title},#{task.description},#{task.status},#{task.createdTime})")
    @Options(useGeneratedKeys = true, keyProperty = "task.id")
    int insertTask(@Param("task") Task task);

    /**
     * 根据 status 返回 task 列表，并以 id 升序排序
     */
    List<Task> getListByStatus(@Param("status") String status);

    /**
     * 根据 id 将任务状态设置为 DONE
     */
    @Update("update task_record set status = 'DONE' where id = #{id} and status = 'PENDING'")
    int setStatusAsDONE(Long id);

    /**
     * 根据 id 删除任务
     */
    @Delete("delete from task_record where id = #{id}")
    int deleteTaskById(Long id);
}
