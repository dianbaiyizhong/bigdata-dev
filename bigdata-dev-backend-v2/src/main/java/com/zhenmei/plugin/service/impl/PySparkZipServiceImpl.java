package com.zhenmei.plugin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhenmei.plugin.entity.PySparkZip;
import com.zhenmei.plugin.mapper.PySparkZipMapper;
import com.zhenmei.plugin.service.PySparkZipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PySparkZipServiceImpl implements PySparkZipService {

    private final PySparkZipMapper pySparkZipMapper;

    @Override
    public PySparkZip upload(String name, String hdfsPath, long fileSize) {
        PySparkZip entity = new PySparkZip();
        entity.setName(name);
        entity.setHdfsPath(hdfsPath);
        entity.setFileSize(fileSize);
        pySparkZipMapper.insert(entity);
        return entity;
    }

    @Override
    public PySparkZip upsert(String name, String hdfsPath, long fileSize) {
        LambdaQueryWrapper<PySparkZip> w = new LambdaQueryWrapper<>();
        w.eq(PySparkZip::getName, name);
        PySparkZip existing = pySparkZipMapper.selectOne(w);
        if (existing != null) {
            existing.setHdfsPath(hdfsPath);
            existing.setFileSize(fileSize);
            existing.setUpdateTime(LocalDateTime.now());
            pySparkZipMapper.updateById(existing);
            return existing;
        }
        return upload(name, hdfsPath, fileSize);
    }

    @Override
    public List<PySparkZip> listAll() {
        LambdaQueryWrapper<PySparkZip> w = new LambdaQueryWrapper<>();
        w.orderByDesc(PySparkZip::getCreateTime);
        return pySparkZipMapper.selectList(w);
    }

    @Override
    public void delete(Long id) {
        pySparkZipMapper.deleteById(id);
    }

    @Override
    public PySparkZip getById(Long id) {
        return pySparkZipMapper.selectById(id);
    }
}
