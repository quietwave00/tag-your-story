package com.tagstory.core.domain.event;

import com.tagstory.core.domain.user.service.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    private User publisher;
    private User subscriber;
    private Object content;
    public static Event from(User publisher, User subscriber, Object content) {
        return Event.builder()
                .publisher(publisher)
                .subscriber(subscriber)
                .content(content)
                .build();
    };
}
