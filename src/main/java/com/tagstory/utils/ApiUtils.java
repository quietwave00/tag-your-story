package com.tagstory.utils;

import com.tagstory.utils.dto.ApiResult;
import lombok.Getter;


@Getter
public class ApiUtils<T> {
    public static <T> ApiResult<T> success(T response) {
        return ApiResult.result(true, response);
    }
}
