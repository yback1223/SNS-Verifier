package org.example.verifier;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.example.common.AuthException;
import org.example.common.AuthProvider;
import org.example.dto.AuthUserDto;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;

@Component
public class GoogleVerifier implements SocialVerifier {

	@Value("${auth.google.client-id}")
	private String clientId;

	@Override
	public boolean support(AuthProvider provider) {
		return provider == AuthProvider.GOOGLE;
	}

	@Override
	public AuthUserDto verify(String token) {
		try {
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
					new NetHttpTransport(),
					new GsonFactory()
			)
					.setAudience(Collections.singletonList(clientId))
					.build();

			GoogleIdToken idToken = verifier.verify(token);
			if (idToken == null) throw new AuthException("Google Id token verification failed");

			GoogleIdToken.Payload payload = idToken.getPayload();

			return new AuthUserDto(
					payload.getSubject(),
					payload.getEmail(),
					(String) payload.get("name"),
					(String) payload.get("picture"),
					AuthProvider.GOOGLE,
					payload
			);
		} catch (Exception e) {
			throw new AuthException("Exception occurred while verifying token: " + e.getMessage());
		}
	}
}
