package com.zhenmei.plugin.controller;

import cn.hutool.core.io.FileUtil;
import com.zhenmei.plugin.entity.PySparkZip;
import com.zhenmei.plugin.response.ApiResponse;
import com.zhenmei.plugin.service.PySparkZipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pyspark")
@RequiredArgsConstructor
public class PySparkZipController {

    private final PySparkZipService pySparkZipService;

    private static final String HDFS_BASE = "/user/pyspark-libs";
    private static final String TEMP_DIR = FileUtil.getTmpDirPath() + File.separator + "pyspark-upload" + File.separator;

    @PostMapping("/upload")
    public ApiResponse<PySparkZip> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".zip")) {
            return ApiResponse.error("只支持 .zip 文件");
        }

        File tempDir = new File(TEMP_DIR);
        if (!tempDir.exists()) tempDir.mkdirs();

        String savedName =  originalName;
        File tempFile = new File(tempDir, savedName);
        file.transferTo(tempFile);

        String hdfsPath = HDFS_BASE + "/" + savedName;
        try {
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", "hdfs://linux001:9820");
            FileSystem fs = FileSystem.get(conf);

            Path hdfsDir = new Path(HDFS_BASE);
            if (!fs.exists(hdfsDir)) {
                fs.mkdirs(hdfsDir);
            }

            Path destPath = new Path(hdfsPath);
            if (fs.exists(destPath)) {
                fs.delete(destPath, false);
                log.info("HDFS 上已存在同名文件，执行替换: {}", hdfsPath);
            }
            fs.copyFromLocalFile(new Path(tempFile.getAbsolutePath()), destPath);
            fs.close();

            log.info("上传 PySpark zip 到 HDFS: {}", hdfsPath);
        } catch (Exception e) {
            log.error("上传到 HDFS 失败", e);
            return ApiResponse.error("上传到 HDFS 失败: " + e.getMessage());
        } finally {
            tempFile.delete();
        }

        PySparkZip entity = pySparkZipService.upsert(originalName, hdfsPath, file.getSize());
        return ApiResponse.success(entity);
    }

    @GetMapping("/list")
    public ApiResponse<List<PySparkZip>> list() {
        return ApiResponse.success(pySparkZipService.listAll());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        PySparkZip entity = pySparkZipService.getById(id);
        if (entity == null) {
            return ApiResponse.error("记录不存在");
        }
        try {
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", "hdfs://linux001:9820");
            FileSystem fs = FileSystem.get(conf);
            Path hdfsPath = new Path(entity.getHdfsPath());
            if (fs.exists(hdfsPath)) {
                fs.delete(hdfsPath, false);
            }
            fs.close();
        } catch (Exception e) {
            log.warn("删除 HDFS 文件失败: {}", entity.getHdfsPath(), e);
        }
        pySparkZipService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<PySparkZip> getById(@PathVariable Long id) {
        return ApiResponse.success(pySparkZipService.getById(id));
    }
}
