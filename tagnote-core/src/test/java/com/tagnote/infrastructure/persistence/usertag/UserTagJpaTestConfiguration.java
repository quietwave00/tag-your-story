package com.tagnote.infrastructure.persistence.usertag;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.repository.BoardRepository;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.repository.UserTagRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EntityScan(basePackageClasses = {
        UserEntity.class, BoardEntity.class, UserTagEntity.class, BoardUserTagEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        BoardRepository.class, UserTagRepository.class, BoardUserTagRepository.class
})
class UserTagJpaTestConfiguration {
}
