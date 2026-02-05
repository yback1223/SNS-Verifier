package back.yback.manager;

import lombok.RequiredArgsConstructor;
import back.yback.common.AuthException;
import back.yback.common.AuthProvider;
import back.yback.dto.AuthUserDto;
import back.yback.verifier.SocialVerifier;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UniversalAuthManager {
	private final List<SocialVerifier> socialVerifiers;

	public AuthUserDto verify(AuthProvider provider , String token) {
		return socialVerifiers.stream()
				.filter(v -> v.support(provider))
				.findFirst()
				.orElseThrow(() -> new AuthException("Not supported login verification."))
				.verify(token);
	}
}
