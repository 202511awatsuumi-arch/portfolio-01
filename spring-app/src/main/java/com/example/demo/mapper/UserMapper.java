package com.example.demo.mapper;

import com.example.demo.model.UserAccount;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    List<UserAccount> selectAll();

    UserAccount selectById(@Param("id") Long id);

    UserAccount selectByUsername(@Param("username") String username);

    int insert(UserAccount user);

    int update(UserAccount user);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int delete(@Param("id") Long id);
}
