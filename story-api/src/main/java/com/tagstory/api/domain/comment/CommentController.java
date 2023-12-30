package com.tagstory.api.domain.comment;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.comment.dto.request.CreateCommentRequest;
import com.tagstory.api.domain.comment.dto.request.CreateReplyRequest;
import com.tagstory.api.domain.comment.dto.request.UpdateCommentRequest;
import com.tagstory.api.domain.comment.dto.response.CommentCountResponse;
import com.tagstory.api.domain.comment.dto.response.CommentResponse;
import com.tagstory.api.domain.comment.dto.response.CommentWithRepliesResponse;
import com.tagstory.core.domain.comment.service.CommentFacade;
import com.tagstory.core.domain.comment.service.Comment;
import com.tagstory.core.domain.comment.service.dto.response.CommentWithReplies;
import com.tagstory.core.utils.api.ApiResult;
import com.tagstory.core.utils.api.ApiUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentFacade commentFacade;

    /*
     * 댓글을 작성한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ApiResult<CommentResponse> create(@RequestBody @Valid CreateCommentRequest request, @CurrentUserId Long userId) throws ExecutionException, InterruptedException {
        CompletableFuture<Comment> comment = commentFacade.create(request.toCommand(userId));
        return ApiUtils.success(CommentResponse.from(comment));
    }

    /*
     * 댓글을 수정한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping
    public ApiResult<CommentResponse> update(@RequestBody @Valid UpdateCommentRequest request) {
        Comment comment = commentFacade.update(request.toCommand());
        return ApiUtils.success(CommentResponse.from(comment));
    }

    /*
     * 댓글을 삭제한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/status/{commentId}")
    public ApiResult<Void> delete(@PathVariable("commentId") Long commentId) {
        commentFacade.delete(commentId);
        return ApiUtils.success();
    }

    /*
     * 게시글에 해당하는 댓글 리스트를 조회한다.
     */
    @GetMapping("/{boardId}/{page}")
    public ApiResult<List<CommentWithRepliesResponse>> getCommentList(@PathVariable("boardId") String boardId,
                                                                      @PathVariable("page") int page) {
        List<CommentWithReplies> commentList = commentFacade.getCommentList(boardId, page);
        return ApiUtils.success(commentList.stream().map(CommentWithRepliesResponse::from).collect(Collectors.toList()));
    }

    /*
     * 댓글에 대한 권한을 확인한다.
     * @return 사용자가 작성한 댓글의 아이디
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/auth/{boardId}")
    public ApiResult<List<Long>> getUserCommentId(@PathVariable("boardId") String boardId, @CurrentUserId Long userId) {
        List<Long> commentIdList = commentFacade.getUserCommentId(boardId, userId);
        return ApiUtils.success(commentIdList);
    }

    /*
     * 게시글에 대한 전체 댓글 개수를 조회한다.
     */
    @GetMapping("/count/{boardId}")
    public ApiResult<CommentCountResponse> getCommentCountByBoardId(@PathVariable("boardId") String boardId) {
        int count = commentFacade.getCommentCountByBoardId(boardId);
        return ApiUtils.success(CommentCountResponse.from(count));
    }


    /*
     * 답글을 작성한다.
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/replies")
    public ApiResult<CommentResponse> createReply(@RequestBody @Valid CreateReplyRequest request, @CurrentUserId Long userId) {
        Comment comment = commentFacade.createReply(request.toCommand(userId));
        return ApiUtils.success(CommentResponse.from(comment));
    }

    /*
     * 답글을 조회한다.
     */
    @GetMapping("/replies/{parentId}/{page}")
    public ApiResult<List<CommentResponse>> getReplyList(@PathVariable("parentId") Long parentId,
                                                         @PathVariable("page") int page) {
        List<Comment> commentList = commentFacade.getReplyList(parentId, page);
        return ApiUtils.success(commentList.stream().map(CommentResponse::from).toList());
    }
}
