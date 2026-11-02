package com.hczq.fq.platform.service;

import com.hczq.fq.common.exception.BusinessException;
import com.hczq.fq.platform.dto.PlatformCreate;
import com.hczq.fq.platform.dto.PlatformRead;
import com.hczq.fq.platform.dto.PlatformUpdate;
import com.hczq.fq.platform.entity.Platform;
import com.hczq.fq.platform.mapper.PlatformMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlatformService {

    private final PlatformMapper platformMapper;

    public PlatformService(PlatformMapper platformMapper) {
        this.platformMapper = platformMapper;
    }

    public List<PlatformRead> list() {
        return platformMapper.selectAll().stream().map(this::toRead).toList();
    }

    @Transactional
    public PlatformRead create(PlatformCreate req) {
        if (platformMapper.countByCode(req.code()) > 0) {
            throw BusinessException.conflict("平台编码已存在（已删除平台的编码也不可复用）");
        }
        Platform p = new Platform();
        p.setCode(req.code());
        p.setName(req.name());
        p.setDesc(req.desc());
        p.setIsSystem(false);
        platformMapper.insert(p);
        return toRead(platformMapper.selectById(p.getId()));
    }

    @Transactional
    public PlatformRead update(Long id, PlatformUpdate req) {
        Platform p = load(id);
        if (req.name() != null) p.setName(req.name());
        if (req.desc() != null) p.setDesc(req.desc());
        platformMapper.update(p);
        return toRead(platformMapper.selectById(id));
    }

    @Transactional
    public void delete(Long id) {
        Platform p = load(id);
        if (p.getIsSystem()) {
            throw BusinessException.conflict("内置平台不可删除");
        }
        long roles = platformMapper.countRoles(id);
        if (roles > 0) {
            throw BusinessException.conflict("平台下仍有 " + roles + " 个角色，请先删除这些角色");
        }
        platformMapper.softDelete(id);
    }

    /** Used by role management: the platform must exist and not be deleted. */
    public Platform require(Long id) {
        return load(id);
    }

    private Platform load(Long id) {
        Platform p = platformMapper.selectById(id);
        if (p == null) {
            throw BusinessException.notFound("平台 " + id + " 不存在");
        }
        return p;
    }

    private PlatformRead toRead(Platform p) {
        return new PlatformRead(p.getId(), p.getCode(), p.getName(), p.getDesc(),
                Boolean.TRUE.equals(p.getIsSystem()), platformMapper.countRoles(p.getId()),
                p.getCreateTime(), p.getUpdateTime());
    }
}
