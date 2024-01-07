package com.tagstory.core.domain.event.publisher;

import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.notification.adaptor.NotificationAdaptor;
import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.core.domain.user.service.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommonEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /*
     * 댓글 알림 이벤트를 발행한다.
     */
    public void onEventFromComment(User user, Board board) {
        Notification notification = Notification.create(user, board, NotificationType.COMMENT);
        eventPublisher.publishEvent(NotificationAdaptor.of(notification));
    }

    /*
     * 좋아요 알림 이벤트를 발행한다.
     */
    public void onEventFromLike(User user, Board board) {
        Notification notification = Notification.create(user, board, NotificationType.LIKE);
        eventPublisher.publishEvent(NotificationAdaptor.of(notification));
    }
}
