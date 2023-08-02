package com.tagstory.annotations;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;


@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "userId")
public @interface CurrentUserId {

}
