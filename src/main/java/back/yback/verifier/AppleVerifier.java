package back.yback.verifier;

import com.fasterxml.jackson.core.type.TypeReference; // [추가] 타입 안전성 위해 필요
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import back.yback.common.AuthException;
import back.yback.common.AuthProvider;
import back.yback.dto.AuthUserDto;
import back.yback.utils.AppleUtils;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파싱용

import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

@Component
public class AppleVerifier implements SocialVerifier {

	@Value("${auth.apple.client-id}")
	private String clientId;

	private final AppleUtils appleUtils;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AppleVerifier(AppleUtils appleUtils) {
		this.appleUtils = appleUtils;
	}


	@Override
	public boolean support(AuthProvider provider) {
		return provider == AuthProvider.APPLE;
	}

	@Override
	public AuthUserDto verify(String token) {
		try {
			String headerJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
			Map<String, String> header = objectMapper.readValue(headerJson, new TypeReference<>() {});

			String kid = header.get("kid");
			if (kid == null) throw new AuthException("No kid in apple token header.");

			PublicKey publicKey = appleUtils.getPublicKey(clientId);

			Claims claims = Jwts.parser()
					.verifyWith(publicKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			Set<String> audiences = claims.getAudience();
			if (audiences == null || !audiences.contains(clientId)) throw new AuthException("Invalid Audience in apple token.");

			return new AuthUserDto(
					claims.getSubject(),
					claims.get("email", String.class),
					null,
					null,
					AuthProvider.APPLE,
					claims
			);
		} catch (Exception e) {
			throw new AuthException(e.getMessage());
		}
	}

	private String getUnsignedToken(String token) {
		String[] splitToken = token.split("\\.");
		if (splitToken.length < 2) throw new AuthException("Invalid token");
		return splitToken[0] + "." + splitToken[1] + ".";
	}
}
