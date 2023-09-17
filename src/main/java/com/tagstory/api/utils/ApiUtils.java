package com.tagstory.api.utils;

import com.tagstory.api.utils.dto.ApiResult;
import lombok.Getter;


@Getter
public class ApiUtils<T> {
    public static <T> ApiResult<T> success(T response) {
        return ApiResult.result(true, response);
    }

    public static ApiResult<Void> success() {
        return ApiResult.result(true);
    }
}
