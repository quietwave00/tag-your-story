package com.tagstory.core.domain.notification;

import com.tagstory.core.domain.BaseTime;
import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.core.domain.user.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

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

    @ColumnDefault("false")
    @Column(nullable = false, columnDefinition = "TINYINT", length = 1)
    private Boolean isRead;


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
     * 비즈니스 로직
     */
    public void setAsRead() {
        this.isRead = true;
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
//                .isRead(this.isRead)
                .build();
    }
}
