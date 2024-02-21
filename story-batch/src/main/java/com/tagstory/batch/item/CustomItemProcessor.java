package com.tagstory.batch.item;

import com.tagstory.core.domain.file.webclient.S3WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CustomItemProcessor implements ItemProcessor<List<String>, List<String>> {

    private final JdbcTemplate jdbcTemplate;
    private final S3WebClient s3WebClient;


    @Override
    public List<String> process(List<String> item) throws Exception {
        deleteFromS3(item);
        return null;
    }

    /*
     * S3에 파일 삭제를 요청한다.
     */
    private void deleteFromS3(List<String> filePathList) {
        filePathList.forEach(s3WebClient::deleteFile);
    }
}
