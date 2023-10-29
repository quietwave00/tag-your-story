package com.tagstory.batch.item;

import com.tagstory.core.domain.file.repository.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;

@Slf4j
public class S3ItemProcessor implements ItemProcessor<List<Long>, List<String>> {

    private FileRepository fileRepository;
    private static final int batchSize = 10;

    @Override
    public List<String> process(List<Long> item) throws Exception {
        log.info("===== S3ItemProcessor =====");
        log.info("item: {}", item.toString());

        return null;
    }



}
