package com.tagstory.core.domain.file.webclient;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.tagstory.core.domain.file.dto.S3File;
import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3WebClient {
    public final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public List<S3File> uploadFiles(List<MultipartFile> multipartFileList) {
        AtomicInteger indexCounter = new AtomicInteger(-1);
        return multipartFileList.stream()
                .map(multipartFile -> uploadFile(multipartFile, indexCounter))
                .collect(Collectors.toList());
    }

    private S3File uploadFile(MultipartFile multipartFile, AtomicInteger indexCounter) {
        try {
            String originalName = multipartFile.getOriginalFilename();
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(multipartFile.getContentType());
            objectMetadata.setContentLength(multipartFile.getSize());

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName, originalName, multipartFile.getInputStream(), objectMetadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead);
            amazonS3Client.putObject(putObjectRequest);

            String filePath = amazonS3Client.getUrl(bucketName, originalName).toString();
            int currentIndex = indexCounter.incrementAndGet();
            return S3File.builder()
                    .originalName(originalName)
                    .filePath(filePath)
                    .index(currentIndex)
                    .build();
        } catch (IOException e) {
            throw new CustomException(ExceptionCode.S3_UPLOAD_EXCEPTION);
        }
    }
}
