package com.tagstory.batch.item;

import com.tagstory.core.domain.file.webclient.S3WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CustomItemWriter implements ItemWriter<List<String>> {
    private final S3WebClient s3WebClient;

    @Override
    public void write(Chunk<? extends List<String>> chunk) {
        deleteFromS3(chunk.getItems());
    }

    /*
     * S3에 파일 삭제를 요청한다.
     */
    private void deleteFromS3(List<? extends List<String>> chunk) {
        List<String> filePathList = chunk.stream()
                .flatMap(List::stream)
                .toList();

        s3WebClient.deleteFile(filePathList);
    }


}
