package com.fukang.knowledge.agent.infrastructure.storage;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * MinIO 文件存储服务
 * <p>封装 MinIO 对象存储的上传操作，负责文件路径生成、桶存在性检查及异常处理，
 * 为知识库管理模块提供可靠的文件持久化能力</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 上传文件到 MinIO
     * <p>自动检查目标桶是否存在，不存在则创建。文件按日期和 UUID 生成唯一存储路径，
     * 避免文件名冲突</p>
     *
     * @param file 前端上传的 MultipartFile 对象
     * @return 文件在 MinIO 中的存储路径（objectName）
     * @throws BaseException 文件上传失败时抛出 FILE_UPLOAD_FAILED
     */
    public String uploadFile(MultipartFile file) {
        String bucketName = minioConfig.getBucketName();
        String objectName = generateObjectName(file.getOriginalFilename());

        try {
            ensureBucketExists(bucketName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("文件已上传到 MinIO: bucket={}, object={}, size={}",
                    bucketName, objectName, file.getSize());
            return objectName;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传到 MinIO 失败: bucket={}, fileName={}", bucketName, file.getOriginalFilename(), e);
            throw new BaseException(ErrorCodeEnum.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 确保目标桶存在
     * <p>检查指定桶是否存在，若不存在则自动创建</p>
     *
     * @param bucketName 桶名称
     * @throws Exception MinIO 操作异常
     */
    private void ensureBucketExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("已创建 MinIO 桶: {}", bucketName);
        }
    }

    /**
     * 删除 MinIO 中的文件
     * <p>根据文件存储路径从 MinIO 中删除指定文件，删除失败时记录日志但不中断业务</p>
     *
     * @param objectName 文件在 MinIO 中的存储路径
     */
    public void deleteFile(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            log.warn("删除文件跳过: 文件路径为空");
            return;
        }
        String bucketName = minioConfig.getBucketName();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("已从 MinIO 删除文件: bucket={}, object={}", bucketName, objectName);
        } catch (Exception e) {
            log.error("从 MinIO 删除文件失败: bucket={}, object={}", bucketName, objectName, e);
        }
    }

    /**
     * 生成唯一的文件存储路径
     * <p>路径格式: documents/{日期}/{UUID}{扩展名}，
     * 确保不同日期、不同文件之间不会产生冲突</p>
     *
     * @param originalFileName 原始文件名，用于提取扩展名
     * @return 生成的文件存储路径
     */
    private String generateObjectName(String originalFileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String extension = "";
        if (originalFileName != null) {
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex != -1) {
                extension = originalFileName.substring(dotIndex);
            }
        }
        return String.format("documents/%s/%s%s", datePath, uuid, extension);
    }
}