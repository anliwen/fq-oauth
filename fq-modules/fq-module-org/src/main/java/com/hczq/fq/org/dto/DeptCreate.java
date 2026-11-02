package com.hczq.fq.org.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeptCreate(
        @NotNull @Size(min = 1, max = 64) String name,
        @JsonAlias("parent_id") Long parentId,
        @Size(max = 64) String leader,
        Integer sort) {

    public DeptCreate {
        if (leader == null) {
            leader = "";
        }
        if (sort == null) {
            sort = 0;
        }
    }
}
