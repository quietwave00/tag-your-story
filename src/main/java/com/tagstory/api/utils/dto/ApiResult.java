package com.tagstory.api.utils.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResult<T> {
    private boolean success;
    private T response;

    public static <T> ApiResult<T> result(boolean success, T response) {
        return new ApiResult<>(success, response);
    }
}