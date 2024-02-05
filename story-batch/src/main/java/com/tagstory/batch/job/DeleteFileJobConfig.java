package com.tagstory.batch.job;

import com.tagstory.batch.item.CustomItemProcessor;
import com.tagstory.batch.mapper.FileListRowMapper;
import com.tagstory.core.domain.file.webclient.S3WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class DeleteFileJobConfig {
    private static final int CHUNK_SIZE = 10;

    private final DeleteFileJobListener listener;
    private final DataSource dataSource;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final S3WebClient s3WebClient;


    /*
     * 삭제된 파일을 지우는 Job 등록
     */
    @Bean
    public Job deleteFileJob() {
        return new JobBuilder("deleteFileJob", jobRepository)
                .start(deleteFile(null))
                .next(deleteFileFromDB(null))
                .listener(listener)
                .build();
    }

    /*
     * 파일을 삭제한다.
     */
    @Bean
    @JobScope
    public Step deleteFile(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return new StepBuilder("deleteFile", jobRepository)
                .<List<String>, List<String>>chunk(CHUNK_SIZE, transactionManager)
                .reader(fileItemReader(requestDate))
                .processor(fileItemProcessor(requestDate))
                .build();
    }

    /*
     * 파일을 DB에서 삭제한다.
     */
    @Bean
    @JobScope
    public Step deleteFileFromDB(@Value("#{jobParameters[requestDate]}") String requestDate) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return new StepBuilder("deleteFileFromDB", jobRepository)
                .tasklet(new DeleteFileFromDBStep(jdbcTemplate), transactionManager)
                .build();
    }

    /*
     * ItemReader
     * 파일 아이디에 해당하는 파일 경로를 읽는다.
     */
    @Bean
    @StepScope
    public JdbcPagingItemReader<List<String>> fileItemReader(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return new JdbcPagingItemReaderBuilder<List<String>>()
                .pageSize(CHUNK_SIZE)
                .fetchSize(CHUNK_SIZE)
                .dataSource(dataSource)
                .rowMapper(new FileListRowMapper())
                .queryProvider(pagingQueryProvider())
                .name("fileItemReader")
                .build();
    }

    /*
     * 파일 경로를 조회하는 쿼리를 돌려준다.
     */
    @Bean
    public PagingQueryProvider pagingQueryProvider() {
        try {
            SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
            factoryBean.setDataSource(dataSource);
            factoryBean.setSelectClause("SELECT file_path");
            factoryBean.setFromClause("FROM files");
            factoryBean.setWhereClause("WHERE status = 'PENDING'");
            factoryBean.setSortKeys(sortKeys());

            return factoryBean.getObject();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /*
     * 데이터를 정렬한다.
     */
    @Bean
    public Map<String, Order> sortKeys() {
        Map<String, Order> sortKeys = new HashMap<>(1);
        sortKeys.put("file_path", Order.ASCENDING);
        return sortKeys;
    }

    /*
     * ItemProcessor
     * DB에서 파일 아이디에 해당하는 행을 지우고,
     * S3로 파일 삭제 요청을 한다.
     */
    @Bean
    @StepScope
    public ItemProcessor<List<String>, List<String>> fileItemProcessor(@Value("#{jobParameters[requestDate]}") String requestDate) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return new CustomItemProcessor(jdbcTemplate, s3WebClient);
    }
}