package com.hczq.fq.user.dto;

import com.hczq.fq.common.api.PartialUpdate;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

/** All fields optional; use {@link #has(String)} to see whether a field was sent. */
public class UserUpdate extends PartialUpdate {

    @Email @Size(max = 128)
    private String email;
    @Size(max = 64)
    private String fullName;
    @Size(max = 32)
    private String employeeNo;
    @Size(max = 64)
    private String title;
    private Long deptId;
    private List<Long> roleIds;
    private String status;

    public String getEmail() { return email; }
    public void setEmail(String v) { mark("email"); this.email = v; }

    public String getFullName() { return fullName; }
    @JsonAlias("full_name")
    public void setFullName(String v) { mark("fullName"); this.fullName = v; }

    public String getEmployeeNo() { return employeeNo; }
    @JsonAlias("employee_no")
    public void setEmployeeNo(String v) { mark("employeeNo"); this.employeeNo = v; }

    public String getTitle() { return title; }
    public void setTitle(String v) { mark("title"); this.title = v; }

    public Long getDeptId() { return deptId; }
    /** Not sent = unchanged; an explicit null takes the user out of their department. */
    @JsonAlias("dept_id")
    public void setDeptId(Long v) { mark("deptId"); this.deptId = v; }

    public List<Long> getRoleIds() { return roleIds; }
    @JsonAlias("role_ids")
    public void setRoleIds(List<Long> v) { mark("roleIds"); this.roleIds = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { mark("status"); this.status = v; }
}
