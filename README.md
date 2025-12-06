# Universal Auth Library (Backend)

**Google** 및 **Apple** 소셜 로그인(OAuth 2.0)의 \*\*ID Token(JWT)\*\*을 검증하고, 표준화된 유저 정보를 반환하는 Spring Boot용 백엔드 라이브러리입니다.

FlutterFlow 등 클라이언트 앱에서 받은 토큰을 이 라이브러리에 전달하기만 하면, 복잡한 검증 로직(서명 확인, 만료 확인, Audience 체크)을 수행하고 유저 데이터를 반환합니다.

## 📋 Features

* **Google Login:** `GoogleIdTokenVerifier`를 사용한 안전한 토큰 검증.
* **Apple Login:**
    * `JJWT`를 활용한 RSA 서명 검증 및 Audience 체크.
    * **Public Key Caching:** 애플 공개키를 메모리에 캐싱하여 불필요한 네트워크 요청 최소화 (성능 최적화).
    * **Auto-Rotation:** 키가 변경되었을 경우 자동으로 감지하여 갱신.
* **Unified Interface:** `UniversalAuthManager` 하나로 모든 소셜 로그인 검증 처리 (Facade Pattern).
* **Raw Attributes Support:** DTO에 없는 플랫폼별 고유 데이터(성별, 생일 등)도 `rawAttributes` 맵을 통해 접근 가능.

## 🛠 Prerequisites

* Java 17+
* Spring Boot 3.x

-----

## 📦 Installation (설치 방법)

이 라이브러리는 현재 로컬 Maven 저장소(`mavenLocal`)를 통해 설치할 수 있습니다.

### 1\. 라이브러리 빌드 (최초 1회)

라이브러리 프로젝트 터미널에서 다음 명령어를 실행하여 로컬 저장소에 배포합니다.

```bash
# Mac / Linux
./gradlew publishToMavenLocal

# Windows
./gradlew.bat publishToMavenLocal
```

### 2\. 내 프로젝트에 추가 (`build.gradle`)

가져다 쓸 프로젝트(예: AR Server)의 `build.gradle`에 다음을 추가합니다.

```groovy
repositories {
    mavenLocal() // 로컬 저장소를 바라보게 설정
    mavenCentral()
}

dependencies {
    // group:name:version은 라이브러리의 build.gradle 설정을 따름
    // 예시: group = 'org.example', version = '1.0-SNAPSHOT'
    implementation 'org.example:universal-auth:1.0-SNAPSHOT'
}
```

-----

## ⚙️ Configuration (설정)

라이브러리가 검증을 수행하기 위해 각 플랫폼의 **Client ID**가 필요합니다.
사용하는 프로젝트의 `src/main/resources/application.yml`에 아래 설정을 필수로 추가해주세요.

```yaml
auth:
  google:
    # Google Cloud Console > 사용자 인증 정보 > 웹 클라이언트 ID
    client-id: "YOUR_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com"
    
  apple:
    # Apple Developer > Identifiers > App IDs (Bundle ID)
    # FlutterFlow 패키지명과 동일 (예: com.example.myapp)
    client-id: "com.your.bundle.id"
```

-----

## 🚀 Usage (사용법)

`LoginController`에서 `UniversalAuthManager`를 주입받아 사용합니다.

### 1\. Controller 예시

```java
import org.example.manager.UniversalAuthManager;
import org.example.common.AuthProvider;
import org.example.dto.AuthUserDto;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UniversalAuthManager authManager;

    @PostMapping("/login/{provider}")
    public ResponseEntity<AuthUserDto> login(
            @PathVariable String provider, 
            @RequestBody String token
    ) {
        // 1. String -> Enum 변환 (GOOGLE, APPLE)
        AuthProvider providerType = AuthProvider.valueOf(provider.toUpperCase());

        // 2. 토큰 검증 수행 (단 한 줄!)
        AuthUserDto user = authManager.verify(providerType, token);

        // 3. 검증된 유저 정보 반환 (이후 DB 조회 및 JWT 발급 로직 연결)
        return ResponseEntity.ok(user);
    }
}
```

### 2\. 반환 데이터 구조 (`AuthUserDto`)

검증이 성공하면 아래와 같은 객체가 반환됩니다. `rawAttributes`를 통해 플랫폼별 원본 데이터를 확인할 수 있습니다.

```java
public record AuthUserDto(
    String oAuthId,       // 플랫폼 고유 식별자 (DB 저장용 Key)
    String email,         // 이메일 (nullable)
    String name,          // 이름 (Apple은 null일 수 있음)
    String pictureUrl,    // 프로필 사진 URL
    AuthProvider provider,// GOOGLE, APPLE...
    Map<String, Object> rawAttributes // [New] 원본 데이터 (추가 정보 필요 시 사용)
) {}
```

-----

## ⚠️ Notes (주의사항)

1.  **Apple Login - Name/Email:**
    * 애플 로그인은 **최초 로그인 시**에만 프론트엔드에 `name` 정보를 줍니다.
    * 백엔드로 전달되는 `id_token`에는 `name` 정보가 포함되어 있지 않으므로, `AuthUserDto`의 `name` 필드는 `null`일 수 있습니다.
2.  **Client ID 일치:**
    * `application.yml`에 적은 Client ID와, 프론트엔드(FlutterFlow)가 사용한 Client ID가 다르면 `Audience Mismatch` 에러가 발생합니다.
    * 안드로이드 앱이라도 **Web Client ID**를 사용하는 것이 구글 권장 사항입니다.

## 🏗 Architecture

이 라이브러리는 **Strategy Pattern**과 **Facade Pattern**을 사용하여 설계되었습니다.

* `UniversalAuthManager`: 외부 요청을 받아 적절한 Verifier에게 위임 (Facade/Context)
* `SocialVerifier`: 각 플랫폼별 검증 로직 구현체 (Strategy)
* `GoogleVerifier` / `AppleVerifier`: 구글/애플 전용 검증 로직

-----

### 📝 License

This project is licensed under the MIT License.