package com.hczq.fq.user.dto;

import com.hczq.fq.common.api.PartialUpdate;

/** All fields optional; use {@link #has(String)} to see whether a field was sent. */
public class RoleUpdate extends PartialUpdate {

    private String name;
    private String desc;

    public String getName() { return name; }
    public void setName(String v) { mark("name"); this.name = v; }

    public String getDesc() { return desc; }
    public void setDesc(String v) { mark("desc"); this.desc = v; }
}
