package com.tagnote.api.domain.notification;

import com.tagnote.api.domain.notification.dto.request.NotificationReadRequest;
import com.tagnote.api.domain.notification.dto.response.NotificationCountResponse;
import com.tagnote.api.domain.notification.dto.response.NotificationResponse;
import com.tagnote.core.utils.api.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "Notification", description = "ROLE_USER 알림 API. 오류 응답의 response.exceptionCode는 custom_error_code이며 OAuth error_code와 다르다.")
public interface NotificationApi {
    @Operation(summary = "실시간 알림 구독", description = "GET /api/notification/subscription. ROLE_USER 필요. text/event-stream. "
            + "초기 init 이벤트 data=subscribe, 이후 Notification 이벤트 data={notificationId,contentId,pubNickname,type,isRead}. "
            + "type은 LIKE 또는 COMMENT. 재접속 누락분은 목록 API로 조회한다. 단일 서버 인스턴스에서만 실시간 전달한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE stream; init 및 Notification 이벤트"),
            @ApiResponse(responseCode = "403", description = "인증 실패", content = @Content)
    })
    SseEmitter subscribe(@Parameter(hidden = true) Long userId);

    @Operation(summary = "알림 목록", description = "GET /api/notification. ROLE_USER 필요. 수신자의 알림을 createdAt DESC로 조회한다. "
            + "0 기반 page, 페이지당 5건. response 항목은 notificationId, contentId(게시글 ID), pubNickname(탈퇴 시 표시 이름), type, isRead다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ApiResult.response에 알림 배열. USER_NOT_FOUND 등은 기존 response.status/exceptionCode로 표현", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "page 누락 또는 형식 오류", content = @Content),
            @ApiResponse(responseCode = "403", description = "인증 실패", content = @Content)
    })
    ApiResult<List<NotificationResponse>> getNotificationList(@Parameter(hidden = true) Long userId,
            @Parameter(name = "page", in = ParameterIn.QUERY, required = true,
                    description = "0 기반 페이지, 5건씩", schema = @Schema(minimum = "0")) int page);

    @Operation(summary = "알림 읽음 처리", description = "PATCH /api/notification. ROLE_USER 필요. JSON body의 notificationId 필수. "
            + "NOTIFICATION_NOT_FOUND와 NO_READ_PERMISSION은 기존 custom_error_code/exceptionCode 체계를 따른다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "빈 ApiResult 성공 또는 기존 오류 response.status/exceptionCode", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류", content = @Content),
            @ApiResponse(responseCode = "403", description = "인증 실패", content = @Content)
    })
    ApiResult<Void> setAsRead(@Parameter(hidden = true) Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true) NotificationReadRequest request);

    @Operation(summary = "전체 알림 수", description = "GET /api/notification/count. ROLE_USER 필요. 읽음 상태와 무관한 전체 건수.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ApiResult.response.count", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "인증 실패", content = @Content)
    })
    ApiResult<NotificationCountResponse> getNotificationCount(@Parameter(hidden = true) Long userId);

    @Operation(summary = "모두 읽음 처리", description = "PATCH /api/notification/all. ROLE_USER 필요. 전체 수와 알림은 삭제하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "빈 ApiResult 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "인증 실패", content = @Content)
    })
    ApiResult<Void> setAllAsRead(@Parameter(hidden = true) Long userId);
}
