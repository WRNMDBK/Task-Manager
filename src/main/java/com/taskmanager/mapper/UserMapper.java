package com.taskmanager.mapper;

import com.taskmanager.entity.dto.User;
import org.apache.ibatis.annotations.*;
import org.springframework.data.annotation.Id;

@Mapper
public interface UserMapper {

    /**
     * 根据'用户名'查完整用户信息
     * @param username 用户名
     * @return User 对象
     */
    @Select("select * from user where username = #{username}")
    @Results(id = "userMap",value = {
            @Result(column = "id", property = "id"),
            @Result(column = "username", property = "username"),
            @Result(column = "password", property = "password"),
            @Result(column = "email", property = "email")
    })
    User selectUserByUsername(@Param("username") String username);

    /**
     * 根据'用户名'查对应的'用户ID'
     * @param username 用户名
     * @return 用户ID
     */
    @Select("select id from user where username = #{username}")
    Long selectIdByUsername(@Param("username") String username);

    /**
     * 根据'邮箱地址'查对应的'用户ID'
     * @param email 邮箱地址
     * @return 用户ID
     */
    @Select("select id from user where email = #{email}")
    Long selectIdByEmail(@Param("email") String email);

    /**
     * 插入新的用户并回填'用户ID'
     * @param user 用户对象
     * @return 执行的行数
     */
    @Insert("insert into user (username,password,email) values (#{user.username},#{user.password},#{user.email})")
    @Options(useGeneratedKeys = true, keyProperty = "user.id")
    int insertUser(@Param("user") User user);

}
