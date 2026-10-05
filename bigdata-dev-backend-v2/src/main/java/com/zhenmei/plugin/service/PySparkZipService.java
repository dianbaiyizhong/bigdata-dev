package com.zhenmei.plugin.service;

import com.zhenmei.plugin.entity.PySparkZip;

import java.util.List;

public interface PySparkZipService {

    PySparkZip upload(String name, String hdfsPath, long fileSize);

    PySparkZip upsert(String name, String hdfsPath, long fileSize);

    List<PySparkZip> listAll();

    void delete(Long id);

    PySparkZip getById(Long id);
}
