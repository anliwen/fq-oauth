package com.hczq.fq.org.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.hczq.fq.common.api.PartialUpdate;
import jakarta.validation.constraints.Size;

/**
 * All fields optional. For {@code parentId} "not sent" means unchanged while an explicit {@code null} moves the
 * department to the root, so use {@link #has(String)} to tell them apart.
 */
public class DeptUpdate extends PartialUpdate {

    @Size(min = 1, max = 64)
    private String name;
    private Long parentId;
    @Size(max = 64)
    private String leader;
    private Integer sort;

    public String getName() { return name; }
    public void setName(String v) { mark("name"); this.name = v; }

    public Long getParentId() { return parentId; }
    @JsonAlias("parent_id")
    public void setParentId(Long v) { mark("parentId"); this.parentId = v; }

    public String getLeader() { return leader; }
    public void setLeader(String v) { mark("leader"); this.leader = v; }

    public Integer getSort() { return sort; }
    public void setSort(Integer v) { mark("sort"); this.sort = v; }
}
