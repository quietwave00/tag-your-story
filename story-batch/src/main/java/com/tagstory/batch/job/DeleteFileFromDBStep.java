package com.tagstory.batch.job;

import com.tagstory.core.common.CommonRedisTemplate;
import com.tagstory.core.config.CacheSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DeleteFileFromDBStep implements Tasklet {

    private final CommonRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info(">>>>> Step2 !!! <<<<<<");
        deleteFromDataBase();
        return RepeatStatus.FINISHED;
    }

    private void deleteFromDataBase() {
        List<Long> fileIdList = redisTemplate.getList("", CacheSpec.FILE_TO_DELETE);
        fileIdList.forEach(fileId -> {
            jdbcTemplate.update("delete from files where file_id = ?", fileId);
        });
        redisTemplate.delete("", CacheSpec.FILE_TO_DELETE);
    }
}
