package com.tagstory.batch.item;

import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class CustomItemWriter implements ItemWriter<List<String>> {
    @Override
    public void write(List<? extends List<String>> items) throws Exception {

    }
}
