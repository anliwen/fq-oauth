package com.hczq.fq.platform.mapper;

import com.hczq.fq.platform.entity.Platform;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** SQL lives in resources/mapper/PlatformMapper.xml. */
public interface PlatformMapper {

    int insert(Platform platform);

    int update(Platform platform);

    int softDelete(@Param("id") Long id);

    /** Not deleted only. */
    Platform selectById(@Param("id") Long id);

    List<Platform> selectAll();

    /** Counts deleted platforms too: a code is never reused, because roles of a deleted platform still point to it. */
    long countByCode(@Param("code") String code);

    /** Roles that still belong to the platform (not deleted). */
    long countRoles(@Param("platformId") Long platformId);
}
