package com.ticketly.mseventseating.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3StorageService s3StorageService;

    @Mock
    private PutObjectResponse putObjectResponse;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private final String TEST_OBJECT_KEY = "test-folder/some-uuid.jpg";

    @BeforeEach
    void setUp() {
        String BUCKET_NAME = "test-bucket";
        ReflectionTestUtils.setField(s3StorageService, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(s3StorageService, "activeProfile", "prod");
    }

    @Test
    void uploadFile_ShouldReturnKeyWhenSuccessful() throws IOException {
        // Arrange
        String TEST_FILE_NAME = "test-file.jpg";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                TEST_FILE_NAME,
                "image/jpeg",
                "test image content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putObjectResponse);
        when(putObjectResponse.eTag()).thenReturn("test-etag");

        // Act
        String TEST_FOLDER = "test-folder";
        String result = s3StorageService.uploadFile(file, TEST_FOLDER);

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith(TEST_FOLDER + "/"));
        assertTrue(result.endsWith(".jpg"));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void generatePresignedUrl_ShouldReturnUrlInProdEnvironment() throws MalformedURLException, URISyntaxException {
        // Arrange
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedGetObjectRequest);
        String TEST_URL = "https://test-bucket.s3.amazonaws.com/test-file.jpg";
        when(presignedGetObjectRequest.url()).thenReturn(new URI(TEST_URL).toURL());


        // Act
        String result = s3StorageService.generatePresignedUrl(TEST_OBJECT_KEY, 60);

        // Assert
        assertEquals(TEST_URL, result);
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void generatePresignedUrl_ShouldReturnDirectUrlInDevEnvironment() {
        // Arrange
        ReflectionTestUtils.setField(s3StorageService, "activeProfile", "dev");
        ReflectionTestUtils.setField(s3StorageService, "endpointUrl", "http://localhost:4566");

        // Act
        String result = s3StorageService.generatePresignedUrl(TEST_OBJECT_KEY, 60);

        // Assert
        assertEquals("http://localhost:4566/test-bucket/test-folder/some-uuid.jpg", result);
        verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void deleteFile_ShouldCallS3ClientWithCorrectParameters() {
        // Arrange
        DeleteObjectResponse mockResponse = mock(DeleteObjectResponse.class);
        // FIX: Use a matcher for the Consumer lambda, not the Request object.
        when(s3Client.deleteObject(any(java.util.function.Consumer.class))).thenReturn(mockResponse);

        // Act
        s3StorageService.deleteFile(TEST_OBJECT_KEY);

        // Assert
        // FIX: Verify the call with the same corrected matcher.
        verify(s3Client).deleteObject(any(java.util.function.Consumer.class));
    }
}