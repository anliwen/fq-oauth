package com.hczq.fq.org.mapper;

import com.hczq.fq.org.entity.Department;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** SQL lives in resources/mapper/DepartmentMapper.xml. */
public interface DepartmentMapper {

    int insert(Department d);

    /** Writes name, parent, leader and sort. */
    int update(Department d);

    int softDelete(@Param("id") Long id);

    /** Not deleted only. */
    Department selectById(@Param("id") Long id);

    List<Department> selectAllActive();

    /**
     * Counts deleted departments too: the unique key on the name covers every row, so the name of a deleted
     * department cannot be reused.
     */
    long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    long countChildren(@Param("id") Long id);

    /** Members that still belong to the department (users not deleted). */
    long countMembers(@Param("id") Long id);

    /** Direct member count per department: rows of {deptId, cnt}. */
    List<Map<String, Object>> countMembersByDept();
}
