package com.ticketly.mseventseating.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Upload a file to S3
     * @param file The file to upload
     * @param folderName Optional folder name for organizational purposes
     * @return The key (path) of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String key = generateKey(folderName, fileExtension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        PutObjectResponse response = s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(file.getBytes())
        );

        log.info("File uploaded successfully to S3. ETag: {}", response.eTag());
        return key;
    }

    /**
     * Generate a presigned URL for accessing a file
     * @param objectKey The key of the file in S3
     * @param expirationInMinutes How long the URL should be valid for
     * @return A presigned URL for the file
     */
    public String generatePresignedUrl(String objectKey, int expirationInMinutes) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationInMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Delete a file from S3
     * @param objectKey The key of the file to delete
     */
    public void deleteFile(String objectKey) {
        s3Client.deleteObject(builder -> builder.bucket(bucketName).key(objectKey));
        log.info("File deleted from S3: {}", objectKey);
    }

    private String generateKey(String folderName, String fileExtension) {
        String uuid = UUID.randomUUID().toString();
        return folderName != null && !folderName.isEmpty()
                ? folderName + "/" + uuid + fileExtension
                : uuid + fileExtension;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
