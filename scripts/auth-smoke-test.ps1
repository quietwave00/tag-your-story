param(
    [Parameter(Mandatory = $true)]
    [string]$TestEmail,

    [Parameter(Mandatory = $true)]
    [string]$TestPassword,

    [string]$BaseUrl = "http://localhost:8080",
    [string]$TestNickname = ("review" + (Get-Date -Format "HHmmss")),
    [string]$SignupCode = "",
    [switch]$RunWithdrawal,
    [string]$LogPath = ("auth-smoke-{0}.log" -f (Get-Date -Format "yyyyMMdd-HHmmss"))
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd('/')
$passCount = 0
$failCount = 0
$script:LastStatus = 0
$script:LastBody = ""
$script:LastJson = $null
$script:LastCookies = @()

Add-Type -AssemblyName System.Net.Http
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.UseCookies = $false
$client = [System.Net.Http.HttpClient]::new($handler)

if ($TestEmail -notmatch '^[^\s@]+@[^\s@]+\.[^\s@]+$') {
    $client.Dispose()
    $handler.Dispose()
    throw "TestEmail must be a valid email address that can receive the signup code."
}

if (-not $TestNickname -or $TestNickname.Length -gt 20 -or $TestNickname -match '\s') {
    $client.Dispose()
    $handler.Dispose()
    throw "TestNickname must contain 1-20 characters without whitespace."
}

function Write-Log([string]$Message) {
    Write-Host $Message
    Add-Content -Path $LogPath -Value $Message -Encoding UTF8
}

function Write-Pass([string]$Message) {
    $script:passCount++
    Write-Log "[PASS] $Message"
}

function Write-Fail([string]$Message) {
    $script:failCount++
    Write-Log "[FAIL] $Message"
}

function Write-Manual([string]$Message) {
    Write-Log "[MANUAL] $Message"
}

function Hide-Tokens([string]$Body) {
    $redacted = $Body -replace '"newAccessToken"\s*:\s*"[^"]+"', '"newAccessToken":"<redacted>"'
    $redacted = $redacted -replace '"newRefreshToken"\s*:\s*"[^"]+"', '"newRefreshToken":"<redacted>"'
    return $redacted -replace '"challengeId"\s*:\s*"[^"]+"', '"challengeId":"<redacted>"'
}

function Invoke-Api {
    param(
        [string]$Label,
        [string]$Method,
        [string]$Path,
        [string]$Body = "",
        [string]$AccessToken = ""
    )

    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Method),
        "$BaseUrl$Path"
    )
    $request.Headers.Accept.ParseAdd("application/json")

    if ($Body) {
        $request.Content = [System.Net.Http.StringContent]::new(
            $Body,
            [System.Text.Encoding]::UTF8,
            "application/json"
        )
    }
    if ($AccessToken) {
        $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new(
            "Bearer",
            $AccessToken
        )
    }

    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    $script:LastStatus = [int]$response.StatusCode
    $script:LastBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    try {
        $script:LastJson = $script:LastBody | ConvertFrom-Json
    } catch {
        $script:LastJson = $null
    }
    try {
        $script:LastCookies = @($response.Headers.GetValues("Set-Cookie"))
    } catch {
        $script:LastCookies = @()
    }

    Write-Log ""
    $safePath = $Path -replace '(?i)(email=)[^&]+', '$1<redacted>'
    Write-Log "[API] $Label`: $Method $safePath"
    Write-Log "[HTTP] $($script:LastStatus)"
    Write-Log "[BODY] $(Hide-Tokens $script:LastBody)"

    if ($script:LastCookies.Count -gt 0) {
        $cookieNames = @($script:LastCookies | ForEach-Object { ($_ -split '=', 2)[0] }) -join ','
        Write-Log "[SET-COOKIE] $cookieNames (values redacted)"
    } else {
        Write-Log "[SET-COOKIE] none"
    }

    $request.Dispose()
    $response.Dispose()
}

function Confirm-Success([string]$Label) {
    if ($script:LastStatus -ge 200 -and $script:LastStatus -lt 300 -and
        $null -ne $script:LastJson -and $script:LastJson.success -eq $true) {
        Write-Pass $Label
    } else {
        Write-Fail $Label
    }
}

function Confirm-Failure([string]$Label) {
    $code = ""
    if ($null -ne $script:LastJson) {
        if ($script:LastJson.response -and $script:LastJson.response.exceptionCode) {
            $code = $script:LastJson.response.exceptionCode
        } elseif ($script:LastJson.exceptionCode) {
            $code = $script:LastJson.exceptionCode
        }
    }

    if ($script:LastStatus -ge 400 -or $code) {
        Write-Pass ($Label + $(if ($code) { " ($code)" } else { "" }))
    } else {
        Write-Fail "$Label`: failure response expected"
    }
}

function Get-CookieToken([string]$Name) {
    foreach ($cookie in $script:LastCookies) {
        if ($cookie -match ("^" + [regex]::Escape($Name) + "=([^;]*)")) {
            return ($matches[1] -replace '^Bearer(\+|%20| )', '')
        }
    }
    return ""
}

function New-Json($Value) {
    return $Value | ConvertTo-Json -Compress
}

Write-Log "TagNote auth smoke test"
Write-Log "base URL: $BaseUrl"
Write-Log "test email: <redacted>"
Write-Log "log file: $LogPath"
Write-Log "Tokens and passwords are not written to this log."

try {
    $openApiResponse = $client.GetAsync("$BaseUrl/v3/api-docs").GetAwaiter().GetResult()
    $openApiBody = $openApiResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $openApi = $openApiBody | ConvertFrom-Json
    $pathNames = @($openApi.paths.PSObject.Properties.Name)
    if ([int]$openApiResponse.StatusCode -eq 200 -and
        $pathNames -contains "/api/auth/login" -and
        $pathNames -contains "/api/user/reissue/refreshToken") {
        Write-Pass "OpenAPI exposes auth endpoints"
    } else {
        Write-Fail "OpenAPI auth contract is unavailable"
        exit 1
    }
    $openApiResponse.Dispose()

    $emailQuery = [Uri]::EscapeDataString($TestEmail)
    Invoke-Api "email availability" "GET" "/api/auth/email-availability?email=$emailQuery"
    Confirm-Success "new email is available"

    $startBody = New-Json @{
        email = $TestEmail
        password = $TestPassword
        passwordConfirm = $TestPassword
    }
    Invoke-Api "signup start" "POST" "/api/auth/signup/email/start" $startBody
    Confirm-Success "signup challenge created"
    $challengeId = $script:LastJson.response.challengeId
    if (-not $challengeId) {
        Write-Fail "challengeId is missing; use a new email and check SMTP/Redis settings"
        exit 1
    }

    if (-not $SignupCode) {
        Write-Manual "Check the test mailbox for the six-digit signup code."
        $SignupCode = Read-Host "Signup code"
    }
    if ($SignupCode -notmatch '^\d{6}$') {
        Write-Fail "signup code must contain exactly six digits"
        exit 1
    }

    $confirmBody = New-Json @{ challengeId = $challengeId; code = $SignupCode }
    Invoke-Api "signup confirm" "POST" "/api/auth/signup/email/confirm" $confirmBody
    Confirm-Success "LOCAL account created"
    $accessToken = Get-CookieToken "Authorization"
    $refreshToken = Get-CookieToken "RefreshToken"
    if ($accessToken -and $refreshToken) {
        Write-Pass "signup returned access and refresh Set-Cookie headers"
    } else {
        Write-Fail "signup Set-Cookie tokens are missing"
        exit 1
    }

    $loginBody = New-Json @{
        email = "  $($TestEmail.ToUpperInvariant())  "
        password = $TestPassword
    }
    Invoke-Api "normalized email login" "POST" "/api/auth/login" $loginBody
    Confirm-Success "normalized email login"
    $loginAccess = Get-CookieToken "Authorization"
    $loginRefresh = Get-CookieToken "RefreshToken"
    if ($loginAccess -and $loginRefresh) {
        $accessToken = $loginAccess
        $refreshToken = $loginRefresh
        Write-Pass "login returned access and refresh Set-Cookie headers"
    } else {
        Write-Fail "login Set-Cookie tokens are missing"
    }

    $nicknameBody = New-Json @{ nickname = $TestNickname }
    Invoke-Api "nickname change" "PATCH" "/api/user/me/nickname" $nicknameBody $accessToken
    Confirm-Success "authenticated nickname change"

    Invoke-Api "refresh token used as access token" "PATCH" "/api/user/me/nickname" $nicknameBody $refreshToken
    Confirm-Failure "refresh token is rejected by a protected API"

    $reissueBody = New-Json @{ refreshToken = $refreshToken }
    Invoke-Api "access token reissue" "POST" "/api/user/reissue/accessToken" $reissueBody
    Confirm-Success "access token reissued from current refresh token"
    if ($script:LastJson.response.newAccessToken) {
        $accessToken = $script:LastJson.response.newAccessToken
    }

    $oldRefresh = $refreshToken
    Invoke-Api "refresh token renewal" "POST" "/api/user/reissue/refreshToken" "" $accessToken
    Confirm-Success "refresh token renewed"
    if ($script:LastJson.response.newRefreshToken) {
        $refreshToken = $script:LastJson.response.newRefreshToken
    }

    Invoke-Api "old refresh token reuse" "POST" "/api/user/reissue/accessToken" (New-Json @{ refreshToken = $oldRefresh })
    Confirm-Failure "previous refresh token rejected after renewal"

    $currentReissueBody = New-Json @{ refreshToken = $refreshToken }
    Invoke-Api "renewed refresh token use" "POST" "/api/user/reissue/accessToken" $currentReissueBody
    Confirm-Success "renewed refresh token can issue an access token"
    if ($script:LastJson.response.newAccessToken) {
        $accessToken = $script:LastJson.response.newAccessToken
    }

    Invoke-Api "logout" "POST" "/api/user/logout" "" $accessToken
    Confirm-Success "logout"

    Invoke-Api "refresh token after logout" "POST" "/api/user/reissue/accessToken" $currentReissueBody
    Confirm-Failure "logout revoked the refresh token"

    Write-Manual "If needed, verify that the access token remains valid until expiry after logout."
    Write-Manual "Google OAuth signup and login require a browser and a real Google test account."
    Write-Manual "SMTP/Redis failures, expiry, rate limits, and concurrency require integration tests."

    if ($RunWithdrawal) {
        Write-Manual "Create a Board or Comment before withdrawal to verify public content preservation."
        $answer = Read-Host "Type yes to withdraw $TestEmail"
        if ($answer -eq "yes") {
            Invoke-Api "login before withdrawal" "POST" "/api/auth/login" $loginBody
            Confirm-Success "login before withdrawal"
            $accessToken = Get-CookieToken "Authorization"
            $refreshToken = Get-CookieToken "RefreshToken"

            Invoke-Api "account withdrawal" "DELETE" "/api/user/me" "" $accessToken
            Confirm-Success "account withdrawn"

            Invoke-Api "withdrawn access token" "PATCH" "/api/user/me/nickname" $nicknameBody $accessToken
            Confirm-Failure "withdrawn access token rejected"

            Invoke-Api "withdrawn refresh token" "POST" "/api/user/reissue/accessToken" (New-Json @{ refreshToken = $refreshToken })
            Confirm-Failure "withdrawn refresh token rejected"

            Invoke-Api "login after withdrawal" "POST" "/api/auth/login" $loginBody
            Confirm-Failure "withdrawn credentials rejected"

            Invoke-Api "email availability after withdrawal" "GET" "/api/auth/email-availability?email=$emailQuery"
            Confirm-Success "withdrawn email is available for a new account"
            Write-Manual "Verify that existing Board/Comment content remains and shows the withdrawn-user label."
            Write-Manual "Re-registration with the same email requires a new code and remains a manual check."
        } else {
            Write-Manual "Withdrawal skipped because confirmation was not provided."
        }
    } else {
        Write-Manual "Withdrawal checks skipped. Re-run with -RunWithdrawal when ready."
    }
} finally {
    $client.Dispose()
    $handler.Dispose()
}

Write-Log ""
Write-Log "Result: $passCount passed, $failCount failed"
if ($failCount -gt 0) {
    exit 1
}
