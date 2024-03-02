package com.tagstory.batch.job.parameter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JobScope
@Getter
@Component
@NoArgsConstructor
public class DeleteFileJobParameter {

    private LocalDateTime dateTime;
    private final int chunkSize = 10;

    @Value("#{jobParameters[date]}")
    public void setDate(String dateTime) {
        this.dateTime = LocalDate.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME).atStartOfDay();
    }
}
