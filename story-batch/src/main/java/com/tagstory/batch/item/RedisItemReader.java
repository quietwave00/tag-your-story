package com.tagstory.batch.item;

import com.tagstory.core.common.CommonRedisTemplate;
import com.tagstory.core.config.CacheSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.database.JpaPagingItemReader;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class RedisItemReader extends JpaPagingItemReader<List<String>> {

    private final CommonRedisTemplate redisTemplate;
    private final EntityManagerFactory entityManagerFactory;

    @Override
    public List<String> read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        log.info("=========== Read FileIdList =============");
        List<Long> fileIdList = redisTemplate.getList("", CacheSpec.FILE_TO_DELETE);
        return getS3URL(fileIdList);
    }

    private List<String> getS3URL(List<Long> fileIdList) {
        List<String> filePathList = new ArrayList<>();
        Query query = getFilePathQuery();
        fileIdList.forEach(fileId -> {
            query.setParameter("fileId", fileId);
            String filePath = (String) query.getSingleResult();
            filePathList.add(filePath);
        });
        return filePathList;
    }

    /*
     * S3 파일 경로를 반환한다.
     */
    private Query getFilePathQuery() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        String sql = "SELECT file_path FROM files WHERE file_id = :fileId";
        return entityManager.createNativeQuery(sql);
    }
 }
