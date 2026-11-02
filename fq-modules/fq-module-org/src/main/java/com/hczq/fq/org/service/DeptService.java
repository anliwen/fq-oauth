package com.hczq.fq.org.service;

import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.org.dto.DeptCreate;
import com.hczq.fq.org.dto.DeptNode;
import com.hczq.fq.org.dto.DeptUpdate;
import com.hczq.fq.org.entity.Department;
import com.hczq.fq.org.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeptService {

    private final DepartmentMapper deptMapper;

    public DeptService(DepartmentMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    /** The department tree with each department's direct members; a node whose parent is gone becomes a root. */
    public List<DeptNode> tree() {
        Map<Long, Long> counts = new HashMap<>();
        for (Map<String, Object> row : deptMapper.countMembersByDept()) {
            counts.put(((Number) row.get("deptId")).longValue(), ((Number) row.get("cnt")).longValue());
        }
        Map<Long, DeptNode> nodes = new LinkedHashMap<>();
        for (Department d : deptMapper.selectAllActive()) {
            nodes.put(d.getId(), toNode(d, counts.getOrDefault(d.getId(), 0L)));
        }
        List<DeptNode> roots = new ArrayList<>();
        for (DeptNode n : nodes.values()) {
            DeptNode parent = n.parentId() == null ? null : nodes.get(n.parentId());
            if (parent != null) {
                parent.children().add(n);
            } else {
                roots.add(n);
            }
        }
        return roots;
    }

    /** Used by user management: the department must exist and not be deleted. */
    public Department require(Long id) {
        return load(id);
    }

    @Transactional
    public DeptNode create(DeptCreate req) {
        if (deptMapper.countByName(req.name(), null) > 0) {
            throw BusinessException.conflict("部门名称已存在");
        }
        if (req.parentId() != null) {
            load(req.parentId());
        }
        Department d = new Department();
        d.setName(req.name());
        d.setParentId(req.parentId());
        d.setLeader(req.leader());
        d.setSort(req.sort());
        deptMapper.insert(d);
        return toNode(deptMapper.selectById(d.getId()), 0);
    }

    @Transactional
    public DeptNode update(Long id, DeptUpdate req) {
        Department d = load(id);
        if (req.has("name") && req.getName() != null && !req.getName().equals(d.getName())) {
            if (deptMapper.countByName(req.getName(), id) > 0) {
                throw BusinessException.conflict("部门名称已存在");
            }
            d.setName(req.getName());
        }
        if (req.has("parentId")) {
            Long target = req.getParentId();
            if (target != null) {
                if (target.equals(id)) {
                    throw BusinessException.conflict("不能移动到自身下");
                }
                load(target);
                if (isDescendant(id, target)) {
                    throw BusinessException.conflict("不能移动到自身或子部门下");
                }
            }
            d.setParentId(target);
        }
        if (req.has("leader") && req.getLeader() != null) d.setLeader(req.getLeader());
        if (req.has("sort") && req.getSort() != null) d.setSort(req.getSort());
        deptMapper.update(d);
        return toNode(deptMapper.selectById(id), deptMapper.countMembers(id));
    }

    @Transactional
    public void delete(Long id) {
        load(id);
        if (deptMapper.countChildren(id) > 0) {
            throw BusinessException.conflict("存在子部门，请先删除或移动子部门");
        }
        long members = deptMapper.countMembers(id);
        if (members > 0) {
            throw BusinessException.conflict("仍有 " + members + " 名成员归属该部门，请先调整");
        }
        deptMapper.softDelete(id);
    }

    private Department load(Long id) {
        Department d = deptMapper.selectById(id);
        if (d == null) {
            throw BusinessException.notFound("部门 " + id + " 不存在");
        }
        return d;
    }

    /**
     * Is {@code nodeId} somewhere below {@code ancestorId}? Walks up from the node; a visited set guards against a
     * cycle that bad data might contain.
     */
    private boolean isDescendant(Long ancestorId, Long nodeId) {
        java.util.Set<Long> seen = new java.util.HashSet<>();
        Long cur = nodeId;
        while (cur != null && seen.add(cur)) {
            Department d = deptMapper.selectById(cur);
            if (d == null || d.getParentId() == null) {
                return false;
            }
            if (d.getParentId().equals(ancestorId)) {
                return true;
            }
            cur = d.getParentId();
        }
        return false;
    }

    private static DeptNode toNode(Department d, long members) {
        return new DeptNode(d.getId(), d.getName(), d.getParentId(), d.getLeader(),
                d.getSort() == null ? 0 : d.getSort(), d.getCreateTime(), d.getUpdateTime(), members);
    }
}
