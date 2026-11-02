package com.hczq.fq.authz.mapper;

import com.hczq.fq.authz.entity.Permission;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** SQL lives in resources/mapper/PermissionMapper.xml. */
public interface PermissionMapper {

    int insert(Permission p);

    /** Only name and sort change; the code, platform, type and parent are fixed once created. */
    int update(Permission p);

    int delete(@Param("id") Long id);

    Permission selectById(@Param("id") Long id);

    Permission selectByPlatformAndCode(@Param("platformId") Long platformId, @Param("code") String code);

    List<Permission> selectByPlatform(@Param("platformId") Long platformId);

    long countChildren(@Param("id") Long id);

    List<Long> selectIdsByRole(@Param("roleId") Long roleId);

    int deleteGrants(@Param("roleId") Long roleId);

    int insertGrants(@Param("roleId") Long roleId, @Param("ids") Collection<Long> ids);

    /**
     * Codes granted to a user on one platform: only through roles of that same platform, so a role on one platform
     * can never grant a code of another.
     */
    List<String> selectGrantedCodes(@Param("userId") Long userId, @Param("platformCode") String platformCode);

    List<String> selectAllCodes(@Param("platformCode") String platformCode);
}
