# Docker Doris 测试环境

使用 **Doris 3.0.7**（Docker 镜像 `apache/doris:fe-3.0.7-rc01` / `be-3.0.7-rc01`）。

## 系统要求

macOS 需先调大 `max_map_count`（Doris BE 依赖 mmap）：

```bash
# 在 docker-desktop 的终端里执行，或宿主机执行：
sudo sysctl -w vm.max_map_count=2000000
```

## 启动

```bash
docker compose up -d
```

## 连接

- **MySQL**: `mysql -h127.0.0.1 -P9030 -uroot`
- **Web UI**: http://localhost:8030

## 验证

```bash
mysql -h127.0.0.1 -P9030 -uroot -e "SHOW PROC '/backends';"
```

看到 `Alive: true` 即正常。

## 停止 & 清理

```bash
docker compose down && rm -rf data
```
