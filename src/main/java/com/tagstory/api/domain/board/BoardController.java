package com.tagstory.api.domain.board;

import com.tagstory.api.annotations.CurrentUserId;
import com.tagstory.api.domain.board.dto.request.CreateBoardRequest;
import com.tagstory.api.domain.board.dto.response.CreateBoard;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.board.dto.response.CreateBoardResponse;
import com.tagstory.core.domain.board.service.BoardFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BoardController {

    private final BoardFacade boardFacade;

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/boards")
    public ApiResult<CreateBoard> create(@RequestBody CreateBoardRequest createBoardRequest, @CurrentUserId Long userId) {
        CreateBoardResponse createBoardResponse = boardFacade.create(createBoardRequest.toCommand(userId));
        return ApiUtils.success(CreateBoard.create(createBoardResponse));
    }
}
