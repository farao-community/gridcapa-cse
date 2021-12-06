/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse.runner.app.services;

import com.farao_community.farao.cse.runner.api.exception.CseInternalException;
import com.farao_community.farao.cse.runner.api.resource.FileResource;
import com.farao_community.farao.cse.runner.app.configurations.MinioConfiguration;
import io.minio.*;
import io.minio.http.Method;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Component
public class MinioAdapter {
    private static final int DEFAULT_DOWNLOAD_LINK_EXPIRY_IN_DAYS = 7;
    private static final Logger LOGGER = LoggerFactory.getLogger(MinioAdapter.class);

    private final MinioClient minioClient;
    private final String bucket;
    private final String basePath;

    public MinioAdapter(MinioConfiguration minioConfiguration, MinioClient client) {
        this.minioClient = client;
        this.bucket = minioConfiguration.getBucket();
        this.basePath = minioConfiguration.getBasePath();
    }

    private void createBucketIfDoesNotExist() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Exception occurred while creating bucket: %s", bucket));
            throw new CseInternalException(String.format("Exception occurred while creating bucket: %s", bucket));
        }

    }

    public void uploadFile(String filePath, InputStream sourceInputStream) {
        String fullPath = String.format("%s/%s", basePath, filePath);
        try {
            createBucketIfDoesNotExist();
            minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(fullPath).stream(sourceInputStream, -1, 50000000).build());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new CseInternalException(String.format("Exception occurred while uploading file: %s, to minio server", filePath));
        }
    }

    public String generatePreSignedUrl(String filePath) {
        String fullPath = String.format("%s/%s", basePath, filePath);
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket).object(fullPath)
                            .expiry(DEFAULT_DOWNLOAD_LINK_EXPIRY_IN_DAYS, TimeUnit.DAYS)
                            .method(Method.GET)
                            .build()
            );
        } catch (Exception e) {
            throw new CseInternalException("Exception in MinIO connection.", e);
        }
    }

    public FileResource generateFileResource(String filePath) {
        try {
            String fullFilePath = String.format("%s/%s", basePath, filePath);
            String filename = FilenameUtils.getName(filePath);
            LOGGER.info("Generates pre-signed URL for file '{}' in Minio bucket '{}'", fullFilePath, bucket);
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucket).object(fullFilePath).expiry(DEFAULT_DOWNLOAD_LINK_EXPIRY_IN_DAYS, TimeUnit.DAYS).method(Method.GET).build());
            return new FileResource(filename, url);
        } catch (Exception e) {
            throw new CseInternalException("Exception in MinIO connection.", e);
        }
    }
}
