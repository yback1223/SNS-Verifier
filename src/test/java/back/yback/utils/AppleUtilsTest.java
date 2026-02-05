package back.yback.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppleUtilsTest {

	@Test
	@DisplayName("애플 서버에서 온 JSON을 파싱해서 PublicKey 객체로 잘 변환하는지 테스트")
	void getPublicKeyTest() throws Exception {
		// [1] 테스트용 정답 키 생성 (RSA 2048)
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair keyPair = kpg.generateKeyPair();
		RSAPublicKey originalKey = (RSAPublicKey) keyPair.getPublic();

		// [2] 정답 키를 애플 JSON 형식으로 변환 (n, e 값 추출 및 인코딩)
		// 애플은 Base64 URL Safe 방식을 씁니다.
		String n = Base64.getUrlEncoder().withoutPadding().encodeToString(originalKey.getModulus().toByteArray());
		String e = Base64.getUrlEncoder().withoutPadding().encodeToString(originalKey.getPublicExponent().toByteArray());
		String kid = "test-kid-01";

		// 애플 서버가 보낼법한 가짜 JSON 응답
		String mockResponse = """
            {
              "keys": [
                {
                  "kty": "RSA",
                  "kid": "%s",
                  "use": "sig",
                  "alg": "RS256",
                  "n": "%s",
                  "e": "%s"
                }
              ]
            }
        """.formatted(kid, n, e);

		// [3] AppleUtils 준비 및 RestTemplate 해킹 (Mocking)
		AppleUtils appleUtils = new AppleUtils();

		// 실제 통신을 하는 RestTemplate 대신 가짜를 넣어치기 (Reflection)
		RestTemplate mockRestTemplate = mock(RestTemplate.class);
		when(mockRestTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

		ReflectionTestUtils.setField(appleUtils, "restTemplate", mockRestTemplate);
		// ObjectMapper 등 다른 필드도 초기화가 필요할 수 있으므로,
		// AppleUtils 코드에서 필드 선언 시 new ObjectMapper()로 초기화되어 있어야 함.

		// [4] 실행
		System.out.println(">>> 가짜 애플 응답: " + mockResponse);
		var resultKey = appleUtils.getPublicKey(kid);

		// [5] 검증 (우리가 만든 원래 키와 복구된 키가 같은지)
		assertNotNull(resultKey);
		assertEquals(originalKey.getModulus(), ((RSAPublicKey) resultKey).getModulus());
		assertEquals(originalKey.getPublicExponent(), ((RSAPublicKey) resultKey).getPublicExponent());

		System.out.println(">>> 테스트 성공! RSA 키 변환 로직이 완벽합니다.");
	}
}