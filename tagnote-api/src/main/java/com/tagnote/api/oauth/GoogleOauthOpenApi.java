package com.tagnote.api.oauth;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleOauthOpenApi {
    // Spring Security owns these endpoints, so no Controller implements their contract.
    @Bean
    OpenApiCustomizer googleOauthPaths() {
        return openApi -> {
            Operation start = new Operation()
                    .tags(java.util.List.of("User"))
                    .security(java.util.List.of())
                    .summary("Google 로그인 시작")
                    .description("인증 불필요. Google 동의 화면으로 이동한다. Request body 없음.")
                    .responses(new ApiResponses()
                            .addApiResponse("302", new ApiResponse().description("Google 인증 페이지로 redirect")));
            openApi.path("/oauth2/authorization/google", new PathItem().get(start));

            ApiResponse callbackRedirect = new ApiResponse()
                    .description("성공: frontend redirect, USER token cookies. "
                            + "실패: frontend redirect, error_code query parameter")
                    .addHeaderObject("Location", new Header().schema(new StringSchema()))
                    .addHeaderObject("Set-Cookie", new Header().schema(new StringSchema()));
            Operation callback = new Operation()
                    .tags(java.util.List.of("User"))
                    .security(java.util.List.of())
                    .summary("Google OAuth callback")
                    .description("Google 서버가 호출한다. 검증된 sub 및 email_verified=true인 email로 "
                            + "ACTIVE USER를 조회 또는 생성하고 commit 후 AccessToken/RefreshToken cookie를 발급한다. "
                            + "기존 sub의 이메일이 달라져도 저장된 이메일은 변경하지 않는다. "
                            + "실패 시 frontend redirect의 error_code는 GOOGLE_EMAIL_UNVERIFIED, GOOGLE_IDENTITY_INVALID, "
                            + "GOOGLE_EMAIL_CONFLICT_LOCAL, GOOGLE_EMAIL_CONFLICT_GOOGLE, USER_INACTIVE, "
                            + "GOOGLE_SIGNUP_CONFLICT 중 하나다. 과거 Pending token은 사용할 수 없다.")
                    .addParametersItem(new Parameter().in("query").name("code").required(true)
                            .description("Google authorization code").schema(new StringSchema()))
                    .addParametersItem(new Parameter().in("query").name("state").required(true)
                            .description("OAuth state").schema(new StringSchema()))
                    .responses(new ApiResponses().addApiResponse("302", callbackRedirect));
            openApi.path("/login/oauth2/code/google", new PathItem().get(callback));
        };
    }
}
