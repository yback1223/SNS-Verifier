package org.example.manager;

import lombok.RequiredArgsConstructor;
import org.example.common.AuthException;
import org.example.common.AuthProvider;
import org.example.dto.AuthUserDto;
import org.example.verifier.SocialVerifier;
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
