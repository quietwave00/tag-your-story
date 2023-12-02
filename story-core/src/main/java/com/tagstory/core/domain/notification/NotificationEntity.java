package com.tagstory.core.domain.notification;

import com.tagstory.core.domain.BaseTime;
import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.core.domain.user.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification")
public class NotificationEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne
    @JoinColumn(name = "publisher_id")
    private UserEntity publisher;

    @ManyToOne
    @JoinColumn(name = "subscriber_id")
    private UserEntity subscriber;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String contentId;


    /*
     * 연관관계 설정
     */
    public void addPublisher(UserEntity userEntity) {
        this.publisher = userEntity;
    }

    public void addSubscriber(UserEntity userEntity) {
        this.subscriber = userEntity;
    }

    /*
     * 형변환
     */
    public Notification toNotification() {
        return Notification.builder()
                .notificationId(this.notificationId)
                .publisher(this.publisher.toUser())
                .subscriber(this.subscriber.toUser())
                .type(this.type)
                .contentId(this.contentId)
                .build();
    }
}
