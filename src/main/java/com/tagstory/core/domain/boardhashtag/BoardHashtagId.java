package com.tagstory.core.domain.boardhashtag;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BoardHashtagId implements Serializable {
    private String boardId;

    private Long hashtagId;
}
