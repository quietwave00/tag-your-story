package com.tagstory.batch.job;

import com.tagstory.batch.job.listener.DeleteFileJobListener;
import com.tagstory.batch.job.step.config.DeleteFileFromDBStepConfig;
import com.tagstory.batch.job.step.config.DeleteFileFromS3StepConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class DeleteFileJobConfig {
    private static final String JOB_NAME = "DELETE_FILE_JOB";

    private final DeleteFileJobListener listener;
    private final DeleteFileFromS3StepConfig s3StepConfig;
    private final DeleteFileFromDBStepConfig dbStepConfig;

    /*
     * 삭제된 파일을 지우는 Job 등록
     */
    @Bean(JOB_NAME)
    public Job deleteFileJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(s3StepConfig.deleteFile(jobRepository, transactionManager))
                .next(dbStepConfig.deleteFileFromDB(jobRepository, transactionManager))
                .listener(listener)
                .build();
    }
}