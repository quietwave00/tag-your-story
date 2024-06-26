package com.tagstory.batch.job.step.config;

import com.tagstory.batch.item.CustomItemWriter;
import com.tagstory.batch.job.parameter.DeleteFileJobParameter;
import com.tagstory.batch.mapper.FileListRowMapper;
import com.tagstory.core.domain.file.webclient.S3WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class DeleteFileFromS3StepConfig {
    private static final String STEP_NAME = "DELETE_FILE_FROM_S3";
    private static final String READER_NAME = STEP_NAME + "_READER";
    private static final String WRITER_NAME = STEP_NAME + "_WRITER";

    private final DeleteFileJobParameter jobParameter;
    private final DataSource dataSource;
    private final S3WebClient s3WebClient;

    /**
     * S3에서 파일을 삭제한다.
     */
    @Bean(STEP_NAME)
    @JobScope
    public Step deleteFile(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("deleteFileFromS3() Executed");
        return new StepBuilder(STEP_NAME, jobRepository)
                .<List<String>, List<String>>chunk(jobParameter.getChunkSize(), transactionManager)
                .reader(fileItemReader())
                .writer(fileItemWriter())
                .build();
    }

    /**
     * 파일 아이디에 해당하는 파일 경로를 읽는다.
     */
    @Bean(READER_NAME)
    @StepScope
    public JdbcPagingItemReader<List<String>> fileItemReader() {
        return new JdbcPagingItemReaderBuilder<List<String>>()
                .pageSize(jobParameter.getChunkSize())
                .fetchSize(jobParameter.getChunkSize())
                .dataSource(dataSource)
                .rowMapper(new FileListRowMapper())
                .queryProvider(pagingQueryProvider())
                .name(READER_NAME)
                .build();
    }

    /**
     * S3로 파일 삭제 요청을 한다.
     */
    @Bean(WRITER_NAME)
    @StepScope
    public ItemWriter<List<String>> fileItemWriter() {
        return new CustomItemWriter(s3WebClient);
    }


    /**
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

    /**
     * 데이터를 정렬한다.
     */
    @Bean
    public Map<String, Order> sortKeys() {
        Map<String, Order> sortKeys = new HashMap<>(1);
        sortKeys.put("file_path", Order.ASCENDING);
        return sortKeys;
    }
}
