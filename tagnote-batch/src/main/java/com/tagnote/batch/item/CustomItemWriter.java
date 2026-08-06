package com.tagnote.batch.item;

import com.tagnote.core.domain.file.storage.FileStorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CustomItemWriter implements ItemWriter<List<String>> {
    private final FileStorageClient fileStorageClient;

    @Override
    public void write(Chunk<? extends List<String>> chunk) {
        deleteFromStorage(chunk.getItems());
    }

    /*
     * 파일 스토리지에 파일 삭제를 요청한다.
     */
    private void deleteFromStorage(List<? extends List<String>> chunk) {
        List<String> filePathList = chunk.stream()
                .flatMap(List::stream)
                .toList();

        fileStorageClient.deleteFiles(filePathList);
    }


}
