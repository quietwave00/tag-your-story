package com.tagstory.core.domain.notification.service;

import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.notification.NotificationEntity;
import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.user.service.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private Long notificationId;

    private User publisher;

    private User subscriber;

    private NotificationType type;

    private String contentId;

    private Boolean isRead;

    /*
     * 비즈니스 로직
     */
    public static Notification create(User user, Board board, NotificationType type) {
        return Notification.builder()
                .publisher(user)
                .subscriber(board.getUser())
                .type(type)
                .contentId(board.getBoardId())
                .isRead(false)
                .build();
    }

    /*
     * 형변환
     */
    public NotificationEntity toEntity() {
        return NotificationEntity.builder()
                .notificationId(this.notificationId)
                .publisher(this.publisher.toEntity())
                .subscriber(this.subscriber.toEntity())
                .type(this.type)
                .contentId(this.contentId)
                .isRead(this.isRead)
                .build();
    }
}
