package com.tagstory.api.domain.comment;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.comment.dto.request.CreateCommentRequest;
import com.tagstory.api.domain.comment.dto.request.CreateReplyRequest;
import com.tagstory.api.domain.comment.dto.request.UpdateCommentRequest;
import com.tagstory.api.domain.comment.dto.response.CommentResponse;
import com.tagstory.core.utils.ApiUtils;
import com.tagstory.core.utils.dto.ApiResult;
import com.tagstory.core.domain.comment.service.CommentFacade;
import com.tagstory.core.domain.comment.service.dto.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentFacade commentFacade;

    /*
     * 댓글을 작성한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping
    public ApiResult<CommentResponse> create(@RequestBody CreateCommentRequest request, @CurrentUserId Long userId) {
        Comment comment = commentFacade.create(request.toCommand(userId));
        return ApiUtils.success(CommentResponse.from(comment));
    }

    /*
     * 댓글을 수정한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PatchMapping
    public ApiResult<CommentResponse> update(@RequestBody UpdateCommentRequest request) {
        Comment comment = commentFacade.update(request.toCommand());
        return ApiUtils.success(CommentResponse.from(comment));
    }

    /*
     * 댓글을 삭제한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PatchMapping("/status/{commentId}")
    public ApiResult<Void> delete(@PathVariable("commentId") Long commentId) {
        commentFacade.delete(commentId);
        return ApiUtils.success();
    }

    /*
     * 게시글에 해당하는 댓글 리스트를 조회한다.
     */
    @GetMapping("/{boardId}")
    public ApiResult<List<CommentResponse>> getCommentList(@PathVariable("boardId") String boardId) {
        List<Comment> commentList = commentFacade.getCommentList(boardId);
        return ApiUtils.success(commentList.stream().map(CommentResponse::from).collect(Collectors.toList()));
    }

    /*
     * 댓글에 대한 권한을 확인한다.
     * @return 사용자가 작성한 댓글의 아이디
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/auth/{boardId}")
    public ApiResult<List<Long>> getUserCommentId(@PathVariable("boardId") String boardId, @CurrentUserId Long userId) {
        List<Long> commentIdList = commentFacade.getUserCommentId(boardId, userId);
        return ApiUtils.success(commentIdList);
    }


    /*
     * 답글을 작성한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/replies")
    public ApiResult<CommentResponse> createReply(@RequestBody CreateReplyRequest request, @CurrentUserId Long userId) {
        Comment comment = commentFacade.createReply(request.toCommand(userId));
        return ApiUtils.success(CommentResponse.from(comment));
    }
}
