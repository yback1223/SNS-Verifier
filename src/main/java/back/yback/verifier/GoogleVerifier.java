package back.yback.verifier;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import back.yback.common.AuthException;
import back.yback.common.AuthProvider;
import back.yback.dto.AuthUserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "GOOGLE_VERIFIER") // 토픽명을 명확히 주면 필터링하기 좋아
@Component
public class GoogleVerifier implements SocialVerifier {

    @Value("${auth.google.client-ids:}")
    private String clientIdsRaw;

    @Override
    public boolean support(AuthProvider provider) {
        return provider == AuthProvider.GOOGLE;
    }

    @Override
    public AuthUserDto verify(String token) {        
        try {
            List<String> audiences = getAudience();

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )
                    .setAudience(audiences)
                    .build();

            GoogleIdToken idToken = verifier.verify(token);
            
            if (idToken == null) {
                log.error("[GoogleAuth] 검증 실패: 유효하지 않은 토큰이거나 Audience 불일치");
                throw new AuthException("Google Id token verification failed");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            return new AuthUserDto(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name"),
                    (String) payload.get("picture"),
                    AuthProvider.GOOGLE,
                    payload
            );
        } catch (AuthException e) {
            log.error("[GoogleAuth] 인증 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[GoogleAuth] 서버 내부 에러: {}", e.getMessage(), e);
            throw new AuthException("Exception occurred while verifying token: " + e.getMessage());
        }
    }

    private List<String> getAudience() {
        // Raw 값이 어떻게 들어오는지 찍어보는 게 제일 중요해
        log.debug("[GoogleAuth] 설정 주입값(clientIdsRaw): '{}'", clientIdsRaw);
        
        List<String> ids = new ArrayList<>();
        if (clientIdsRaw != null && !clientIdsRaw.isBlank()) {
            for (String id : clientIdsRaw.split(",")) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty()) ids.add(trimmed);
            }
        }

        if (ids.isEmpty()) {
            log.error("[GoogleAuth] 설정 에러: auth.google.client-ids 값이 비어있습니다.");
            throw new AuthException("Google client id is not configured");
        }
        return ids;
    }
}