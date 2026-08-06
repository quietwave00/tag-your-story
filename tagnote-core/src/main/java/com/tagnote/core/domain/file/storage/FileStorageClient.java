package com.tagnote.core.domain.file.storage;

import com.tagnote.core.domain.file.dto.StoredFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageClient {
    List<StoredFile> uploadFiles(List<MultipartFile> multipartFileList);

    void deleteFiles(List<String> filePathList);
}
