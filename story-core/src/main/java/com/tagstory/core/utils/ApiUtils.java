package com.tagstory.core.utils;

import com.tagstory.core.utils.dto.ApiResult;
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
