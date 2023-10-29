package com.tagstory.batch.item;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

@Slf4j
public class S3ItemWriter implements ItemWriter<List<String>> {
    @Override
    public void write(List<? extends List<String>> items) throws Exception {
        log.info("===== ItemWriter ====");
        log.info("items: {}", items.toString());
    }
}
