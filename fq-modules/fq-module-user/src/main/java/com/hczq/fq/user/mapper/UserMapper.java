package com.hczq.fq.user.mapper;

import com.hczq.fq.user.entity.User;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** SQL lives in resources/mapper/UserMapper.xml. */
public interface UserMapper {

    int insert(User user);

    /** Persists every editable column; the service fills the entity from the current row plus the changes. */
    int update(User user);

    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("isActive") boolean isActive);

    /** Stores a new password hash and bumps token_version. */
    int updatePassword(@Param("id") Long id, @Param("hash") String hash);

    int updateLastLogin(@Param("id") Long id, @Param("at") LocalDateTime at);

    int softDelete(@Param("id") Long id);

    User selectById(@Param("id") Long id);

    /** Includes soft-deleted rows: at login time nothing is known about the account yet. */
    User selectByUsername(@Param("username") String username);

    User selectByEmail(@Param("email") String email);

    User selectByEmployeeNo(@Param("employeeNo") String employeeNo);

    List<User> selectPage(@Param("keyword") String keyword, @Param("deptId") Long deptId, @Param("status") String status,
                          @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("keyword") String keyword, @Param("deptId") Long deptId, @Param("status") String status);

    int deleteRoles(@Param("userId") Long userId);

    int insertRoles(@Param("userId") Long userId, @Param("roleIds") Collection<Long> roleIds);
}
