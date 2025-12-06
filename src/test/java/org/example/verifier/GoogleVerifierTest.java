package org.example.verifier;

import org.example.common.AuthProvider;
import org.example.dto.AuthUserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class GoogleVerifierTest {

	// 테스트용 변수 준비
	// 1. 구글 Cloud Console에서 만든 내 클라이언트 ID
	// (주의: Playground 토큰을 쓸 거면 Playground 설정에서 내 ID를 넣고 발급받아야 함!)
	String MY_CLIENT_ID = "705896167244-98706lo6nubhn6ljgnjluu600o1cgmsc.apps.googleusercontent.com";

	// 2. OAuth Playground에서 방금 복사해온 따끈따끈한 id_token (1시간 유효)
	String REAL_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImQ1NDNlMjFhMDI3M2VmYzY2YTQ3NTAwMDI0NDFjYjIxNTFjYjIzNWYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI3MDU4OTYxNjcyNDQtOTg3MDZsbzZudWJobjZsamduamx1dTYwMG8xY2dtc2MuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI3MDU4OTYxNjcyNDQtOTg3MDZsbzZudWJobjZsamduamx1dTYwMG8xY2dtc2MuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDQ2NjU3Mzg5MzEzNDMxMzc0MjQiLCJlbWFpbCI6InlveW9iMTIyM0BnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiYXRfaGFzaCI6IlZhUC1BRlkyc3haRm5fZWc0bk4tWnciLCJuYW1lIjoiWW9uZyBNaW4gQmFjayIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BQ2c4b2NMUjltWXRKYU5icHNLVDVpbG9qNk1wU3JIWk03VHhual9YWXdTVzJQVnVDeTVwb0E9czk2LWMiLCJnaXZlbl9uYW1lIjoiWW9uZyBNaW4iLCJmYW1pbHlfbmFtZSI6IkJhY2siLCJpYXQiOjE3NjUwMTQxNTEsImV4cCI6MTc2NTAxNzc1MX0.uy466YcYNL8utVgODld20TuH229k2GbkX4mhGlORXAok7g0GYmOA8K83W4XeSo70HLgBk0PHALV2Ovqo4bMmLpxWM0Hxu-sBGy7S4XL7tJzEHxCv0XsDVJnE-LfNpLARWv6fwFE_rplWnOIDitn9i2yC6FvLOBUcRTrLryCYOOmCbNu4q4-4qUWr7ieK3pIv3TZRy6EfK-Ik06CAqL3I1ZAa76PuL7rVSiohgOd2q9m_d7bynyOxFGsQ_YQ24pt7E-wJdFUyJO2VfeIu1z4s2cBYeVIEYu5gi3glNtykHGBNn1PGrjmbeNaAPHg2BLbiUtrppz4t5XLFioSIN6pDFw";

	@Test
	@DisplayName("구글 제공자(GOOGLE)만 지원해야 한다")
	void support() {
		GoogleVerifier verifier = new GoogleVerifier();

		// GOOGLE일 때는 true여야 함
		assertTrue(verifier.support(AuthProvider.GOOGLE));

		// KAKAO일 때는 false여야 함
		assertFalse(verifier.support(AuthProvider.KAKAO));
	}

	@Test
	@DisplayName("실제 구글 토큰을 넣으면 유저 정보가 반환되어야 한다")
	void verify() {
		// 1. 객체 생성
		GoogleVerifier verifier = new GoogleVerifier();

		// 2. @Value("${auth.google.client-id}")가 작동 안 하므로 강제 주입
		// (스프링 컨텍스트 없이 테스트하기 위함)
		ReflectionTestUtils.setField(verifier, "clientId", MY_CLIENT_ID);

		System.out.println(">>> 검증 시작...");

		// 3. 검증 실행
		AuthUserDto user = verifier.verify(REAL_TOKEN);

		// 4. 결과 확인 (Assertions)
		assertNotNull(user); // 유저 객체가 비어있으면 안 됨
		assertEquals(AuthProvider.GOOGLE, user.provider()); // 제공자가 구글이어야 함

		// 5. 눈으로 확인
		System.out.println(">>> 검증 성공!");
		System.out.println("User ID: " + user.oAuthId());
		System.out.println("Email: " + user.email());
		System.out.println("Name: " + user.name());
	}
}