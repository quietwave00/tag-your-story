package com.tagstory.batch.item;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;

@Slf4j
public class CustomItemProcessor implements ItemProcessor<List<String>, List<String>> {
    @Override
    public List<String> process(List<String> item) throws Exception {
        log.info(">>>>>>" + item.toString());
        return null;
    }
}
