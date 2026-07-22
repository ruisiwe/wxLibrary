# COS 与文档转换配置模板

将以下内容合并到后端外部配置中。桶名称填写 `file-1302202133`，不要填写完整 HTTPS 地址；密钥通过运行环境变量注入，不要写死或提交到 Git。

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

library:
  cos:
    region: ap-beijing
    bucket: file-1302202133
    secret-id: ${TENCENT_COS_SECRET_ID}
    secret-key: ${TENCENT_COS_SECRET_KEY}
  document-conversion:
    executable: "C:/Program Files/LibreOffice/program/soffice.exe"
    temp-directory: "D:/wx-library-temp"
    timeout-seconds: 120
    max-input-bytes: 104857600
    max-output-bytes: 104857600
```

`temp-directory` 必须是后端服务账号可读写的独立真实目录，不能是符号链接。上线前请按实际安装位置修改 LibreOffice 可执行文件路径，并确保临时目录磁盘空间足够。
