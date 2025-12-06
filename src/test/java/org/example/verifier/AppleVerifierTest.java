package org.example.verifier;

import io.jsonwebtoken.Jwts;
import org.example.common.AuthProvider;
import org.example.dto.AuthUserDto;
import org.example.utils.AppleUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {AppleVerifier.class})
@TestPropertySource(properties = {
		"auth.apple.client-id=com.test.app"
})
class AppleVerifierTest {

	@Autowired
	private AppleVerifier appleVerifier;

	@MockitoBean
	private AppleUtils appleUtils;

	@Test
	@DisplayName("애플 제공자(APPLE)만 지원해야 한다")
	void support() {
		assertTrue(appleVerifier.support(AuthProvider.APPLE));
		assertFalse(appleVerifier.support(AuthProvider.GOOGLE));
	}

	@Test
	@DisplayName("가짜 토큰을 만들어서 애플 로그인 검증 로직을 테스트한다")
	void verify() throws Exception {
		// [1] RSA 키 쌍 생성
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair keyPair = kpg.generateKeyPair();

		// [2] 가짜(Mock) AppleUtils 설정
		when(appleUtils.getPublicKey(anyString())).thenReturn(keyPair.getPublic());

		// [3] 가짜 애플 토큰 생성
		long now = System.currentTimeMillis();
		String fakeToken = Jwts.builder()
				.header().add("kid", "test-kid").and()
				.issuer("https://appleid.apple.com")
				.audience().add("com.test.app").and()
				.subject("apple-user-1234")
				.claim("email", "test@apple.com")
				.expiration(new Date(now + 1000 * 60))
				.signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
				.compact();

		// [4] 검증 실행
		AuthUserDto user = appleVerifier.verify(fakeToken);

		// [5] 결과 확인
		assertNotNull(user);
		assertEquals("apple-user-1234", user.oAuthId());
		assertEquals("test@apple.com", user.email());
		assertEquals(AuthProvider.APPLE, user.provider());

		System.out.println(">>> 테스트 성공!");
	}
}