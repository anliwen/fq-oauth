package com.hczq.fq.authz.dto;

import com.hczq.fq.authz.entity.Permission;

import java.util.ArrayList;
import java.util.List;

/** One node of a platform's permission tree. */
public record PermissionNode(
        Long id,
        Long platformId,
        Long parentId,
        String code,
        String name,
        String type,
        int sort,
        boolean isSystem,
        List<PermissionNode> children) {

    public static PermissionNode of(Permission p) {
        return new PermissionNode(p.getId(), p.getPlatformId(), p.getParentId(), p.getCode(), p.getName(),
                p.getType(), p.getSort() == null ? 0 : p.getSort(), Boolean.TRUE.equals(p.getIsSystem()),
                new ArrayList<>());
    }
}
