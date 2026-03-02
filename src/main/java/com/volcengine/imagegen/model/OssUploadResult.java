package com.volcengine.imagegen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for OSS upload result
 */
public record OssUploadResult(
        @JsonProperty("file_name")
        String fileName,

        @JsonProperty("file_key")
        String fileKey,

        @JsonProperty("file_url")
        String fileUrl,

        @JsonProperty("file_size")
        Long fileSize,

        @JsonProperty("content_type")
        String contentType,

        @JsonProperty("bucket")
        String bucket
) {
    public OssUploadResult {
        if (fileSize == null) {
            fileSize = 0L;
        }
    }
}
