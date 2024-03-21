package com.tagstory.batch.job.parameter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@JobScope
@NoArgsConstructor
@Getter
@Component
@Slf4j
public class DeleteFileJobParameter {

    private LocalDate date;
    private final int chunkSize = 10;

    @Value("#{jobParameters[date]}")
    public void setDate(String date) {
        this.date = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
    }
}
