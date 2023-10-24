package com.tagstory.core.utils.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResult<T> {
    private boolean success;
    private T response;

    private ApiResult(boolean success) {
        this.success = success;
    }

    public static <T> ApiResult<T> result(boolean success, T response) {
        return new ApiResult<>(success, response);
    }

    public static <T> ApiResult<T> result(boolean success) {
        return new ApiResult<>(success);
    }
}