package com.hczq.fq.user.mapper;

import com.hczq.fq.user.entity.Role;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** SQL lives in resources/mapper/RoleMapper.xml. */
public interface RoleMapper {

    int insert(Role role);

    /** Only name and description change; the platform and the code are fixed once created. */
    int update(Role role);

    int softDelete(@Param("id") Long id);

    Role selectById(@Param("id") Long id);

    Role selectByPlatformAndCode(@Param("platformId") Long platformId, @Param("code") String code);

    List<Role> selectAll();

    List<Role> selectByIds(@Param("ids") Collection<Long> ids);

    /** The roles a user holds on every platform, in ascending role id order. */
    List<Role> selectByUserId(@Param("userId") Long userId);

    /**
     * Codes of the roles a user holds on one platform. Authorization checks for this centre use only the centre's own
     * platform, so a role named "admin" on another platform can never pass the centre's superadmin check.
     */
    List<String> selectCodesByUserIdInPlatform(@Param("userId") Long userId, @Param("platformCode") String platformCode);

    long countUsers(@Param("roleId") Long roleId);
}
