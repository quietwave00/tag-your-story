package com.tagstory.batch.job;

import com.tagstory.batch.item.CustomItemProcessor;
import com.tagstory.batch.mapper.FileListRowMapper;
import com.tagstory.core.common.CommonRedisTemplate;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.file.webclient.S3WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class DeleteFileJobConfig {
    private static final int chunkSize = 10;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final CommonRedisTemplate redisTemplate;
    private final S3WebClient s3WebClient;
    private final DataSource dataSource;

    @Bean
    public Job deleteFileJob() {
        return jobBuilderFactory.get("deleteFileJob")
                .start(deleteFile())
                .next(deleteFileFromDB())
                .build();
    }

    /*
     * 파일을 삭제한다.
     */
    @Bean
    public Step deleteFile() {
        return stepBuilderFactory.get("deleteFile")
                .<List<String>, List<String>>chunk(chunkSize)
                .reader(fileItemReader())
                .processor(fileItemProcessor(redisTemplate))
                .writer(fileItemWriter())
                .build();
    }

    /*
     * 파일을 DB에서 삭제한다.
     */
    @Bean
    public Step deleteFileFromDB() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return stepBuilderFactory.get("deleteFileFromDB")
                .tasklet(new DeleteFileFromDBStep(redisTemplate, jdbcTemplate))
                .build();
    }

    /*
     * ItemReader
     * 파일 아이디에 해당하는 파일 경로를 읽는다.
     */
    @Bean
    public JdbcPagingItemReader<List<String>> fileItemReader() {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("fileIdList", redisTemplate.getList("", CacheSpec.FILE_TO_DELETE));

        return new JdbcPagingItemReaderBuilder<List<String>>()
                .pageSize(chunkSize)
                .fetchSize(chunkSize)
                .dataSource(dataSource)
                .rowMapper(new FileListRowMapper())
                .queryProvider(pagingQueryProvider())
                .parameterValues(parameterValues)
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
            factoryBean.setWhereClause("WHERE file_id IN (:fileIdList)");
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
    public ItemProcessor<List<String>, List<String>> fileItemProcessor(CommonRedisTemplate redisTemplate) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return new CustomItemProcessor(redisTemplate, jdbcTemplate, s3WebClient);
    }

    /*
     * ItemWriter
     *
     */
    @Bean
    public ItemWriter<List<String>> fileItemWriter() {
        return new ItemWriter<>() {
            @Override
            public void write(List<? extends List<String>> items) throws Exception {
                log.info(">>>>>>>ItemWriter!!<<<<<<");
            }
        };
    }
}