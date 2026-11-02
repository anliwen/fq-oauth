package com.hczq.fq.org.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A department with its sub-departments and its direct member count. */
public record DeptNode(
        Long id,
        String name,
        Long parentId,
        String leader,
        int sort,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        long userCount,
        List<DeptNode> children) {

    public DeptNode(Long id, String name, Long parentId, String leader, int sort,
                    LocalDateTime createTime, LocalDateTime updateTime, long userCount) {
        this(id, name, parentId, leader, sort, createTime, updateTime, userCount, new ArrayList<>());
    }
}
