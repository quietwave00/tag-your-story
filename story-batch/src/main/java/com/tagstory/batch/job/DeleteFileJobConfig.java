package com.tagstory.batch.job;

import com.tagstory.batch.item.RedisItemReader;
import com.tagstory.batch.item.S3ItemWriter;
import com.tagstory.core.common.CommonRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class DeleteFileJobConfig {
    private static final int pageSize = 10;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private final CommonRedisTemplate redisTemplate;

    @Bean
    public Job deleteFileJob() {
        return jobBuilderFactory.get("deleteFileJob")
                .start(getS3URLFromFileId())
                    .on("FAILED") //contribution.setExitStatus(ExitStatus.FAILED) 조정 필요
                    .fail()
                .from(getS3URLFromFileId())
                    .on("*")
                    .to(deleteFileFromTableStep())
                .from(deleteFileFromTableStep())
                    .on("*")
                    .to(deleteFileFromS3Step())
                .from(deleteFileFromTableStep())
                    .on("*")
                    .end()
                .end()
                .incrementer(new RunIdIncrementer())
                .build();
    }

    /*
     * 삭제된 파일 아이디의 리스트를 가져오고
     * 해당 아이디의 S3 URL을 가져온다.
     */
    @Bean
    public Step getS3URLFromFileId() {
//        return stepBuilderFactory.get("getS3URLFromFileId")
//                .<List<String>, List<String>>chunk(10)
//                .reader(redisItemReader())
//                .writer(s3ItemWriter())
//                .build();
        return stepBuilderFactory.get("getS3URLFromFileId")
                .tasklet((contribution, chunkContext) -> {
                    log.info("!!!Executing getS3URLFromFileId!!!");
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

    /*
     * getS3URLFromFileId()의 ItemReader, ItemProcessor, ItemWriter
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<List<String>> redisItemReader() {
        JpaPagingItemReader<List<String>> itemReader = new RedisItemReader(redisTemplate, entityManagerFactory);
        itemReader.setPageSize(pageSize);
        return itemReader;
    }

    @Bean
    public ItemWriter<List<String>> s3ItemWriter() {
        return new S3ItemWriter();
    }
}