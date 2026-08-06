package com.tagnote.batch.job;

import com.tagnote.batch.job.listener.DeleteFileJobListener;
import com.tagnote.batch.job.step.config.DeleteFileFromDBStepConfig;
import com.tagnote.batch.job.step.config.DeleteFileFromStorageStepConfig;
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
    private final DeleteFileFromStorageStepConfig storageStepConfig;
    private final DeleteFileFromDBStepConfig dbStepConfig;

    /*
     * 삭제된 파일을 지우는 Job 등록
     */
    @Bean(JOB_NAME)
    public Job deleteFileJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(storageStepConfig.deleteFile(jobRepository, transactionManager))
                .next(dbStepConfig.deleteFileFromDB(jobRepository, transactionManager))
                .listener(listener)
                .build();
    }
}
