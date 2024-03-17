package com.tagstory.batch.job.step.config;

import com.tagstory.batch.job.step.DeleteFileFromDBStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class DeleteFileFromDBStepConfig {
    private static final String STEP_NAME = "DELETE_FILE_FROM_DB";

    private final DataSource dataSource;

    @Bean(STEP_NAME)
    @JobScope
    public Step deleteFileFromDB(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return new StepBuilder("deleteFileFromDB", jobRepository)
                .tasklet(new DeleteFileFromDBStep(jdbcTemplate), transactionManager)
                .build();
    }
}
