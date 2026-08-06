package com.tagnote.core.domain.file.storage;

import com.tagnote.core.domain.file.dto.StoredFile;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Component
public class UnsupportedFileStorageClient implements FileStorageClient {

    @Override
    public List<StoredFile> uploadFiles(List<MultipartFile> multipartFileList) {
        throw new CustomException(ExceptionCode.FILE_STORAGE_UNAVAILABLE);
    }

    @Override
    public void deleteFiles(List<String> filePathList) {
        log.warn("File storage is not configured. Skip deleting {} files.", filePathList.size());
    }
}
