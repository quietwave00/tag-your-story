package com.tagstory.batch.job.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@RequiredArgsConstructor
public class DeleteFileFromDBStep implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        deleteFromDataBase();
        return RepeatStatus.FINISHED;
    }

    private void deleteFromDataBase() {
        jdbcTemplate.update("delete from files where status = 'PENDING'");
    }
}
