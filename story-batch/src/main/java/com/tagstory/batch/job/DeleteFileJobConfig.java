package com.tagstory.batch.job;

import com.tagstory.core.domain.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class DeleteFileJobConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final FileService fileService;

    @Bean
    public Job deleteFileJob() {
        return jobBuilderFactory.get("deleteFileJob")
                .start(getFileIdListStep())
                    .on("FAILED") //contribution.setExitStatus(ExitStatus.FAILED) 조정 필요
                    .fail()
                .from(getFileIdListStep())
                    .on("*")
                    .to(deleteFileFromTableStep())
                .from(deleteFileFromTableStep())
                    .on("*")
                    .to(deleteFileFromS3Step())
                .from(deleteFileFromTableStep())
                    .on("*")
                    .end()
                .end()
                .build();
    }

    @Bean
    public Step getFileIdListStep() {
        return stepBuilderFactory.get("getFileIdListStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info("!!!Executing getFileIdListStep!!!");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step deleteFileFromTableStep() {
        return stepBuilderFactory.get("deleteFileFromTableStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info("!!!Executing deleteFileFromTableStep!!!");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step deleteFileFromS3Step() {
        return stepBuilderFactory.get("deleteFileFromS3Step")
                .tasklet((contribution, chunkContext) -> {
                    log.info("!!!Executing deleteFileFromS3Step!!!");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
