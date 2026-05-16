SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
create database bigdata_dev_db;
use bigdata_dev_db;
CREATE TABLE IF NOT EXISTS t_spark_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(255),
    job_type VARCHAR(32) DEFAULT 'JAR',
    jar_path VARCHAR(512),
    script_path VARCHAR(512),
    py_zip_path VARCHAR(512),
    main_class VARCHAR(512),
    app_args VARCHAR(1024),
    app_id VARCHAR(128),
    status VARCHAR(32) DEFAULT 'SUBMITTING',
    spark_properties LONGTEXT,
    deploy_mode VARCHAR(32) DEFAULT 'cluster',
    master VARCHAR(64) DEFAULT 'yarn',
    driver_memory INT,
    driver_cores INT,
    executor_memory INT,
    executor_cores INT,
    num_executors INT,
    dependency_ids VARCHAR(1024),
    error_msg LONGTEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_app_jar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    jar_path VARCHAR(512),
    description VARCHAR(512),
    file_size BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_dependency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(512),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_dependency_jar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dependency_id BIGINT NOT NULL,
    name VARCHAR(255),
    jar_path VARCHAR(512),
    file_size BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_pyspark_zip (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(512),
    hdfs_path VARCHAR(512),
    file_size BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
  