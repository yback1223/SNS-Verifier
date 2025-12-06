package org.example.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.common.AuthException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class AppleUtils {

	// 1. 애플의 공개키 목록을 주는 URL
	private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";

	// 2. 도구들 초기화
	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	// 3. 캐싱 변수
	private List<JsonNode> cachedKeys = new ArrayList<>();
	private long lastFetchTime = 0;
	private static final long CACHE_TTL = 1000 * 60 * 60 * 24; // 24시간

	public PublicKey getPublicKey(String kid) {
		try {
			// 캐시 유효성 체크
			boolean isExpired = (System.currentTimeMillis() - lastFetchTime > CACHE_TTL);

			// 캐시가 비었거나 만료됐으면 새로고침
			if (cachedKeys.isEmpty() || isExpired) fetchApplePublicKeys();

			// 1차 시도: 캐시에서 키 찾기
			PublicKey publicKey = findKeyInCache(kid);
			if (publicKey != null) return publicKey;

			// 2차 시도 (캐시 미스 대응):
			// 만약 캐시가 만료되지 않았는데도 키를 못 찾았다면?
			// 애플이 방금 키를 바꿨을 수도 있으니 강제로 한 번 더 새로고침 해봄.
			if (!isExpired) {
				fetchApplePublicKeys();
				publicKey = findKeyInCache(kid);
				if (publicKey != null) return publicKey;
			}

			throw new AuthException("일치하는 애플 공개키를 찾을 수 없습니다. kid: " + kid);

		} catch (AuthException e) {
			throw e;
		} catch (Exception e) {
			throw new AuthException("애플 공개키 로딩 실패: " + e.getMessage());
		}
	}

	// 캐시된 목록에서 kid와 일치하는 키를 찾아 RSA 객체로 변환하는 메서드
	private PublicKey findKeyInCache(String kid) throws Exception {
		for (JsonNode key : cachedKeys) {
			if (key.get("kid").asText().equals(kid)) {
				String n = key.get("n").asText();
				String e = key.get("e").asText();

				// n, e 값을 이용해 RSA 공개키 객체 생성 (수학 공식)
				byte[] nBytes = Base64.getUrlDecoder().decode(n);
				byte[] eBytes = Base64.getUrlDecoder().decode(e);

				BigInteger nBigInt = new BigInteger(1, nBytes);
				BigInteger eBigInt = new BigInteger(1, eBytes);

				RSAPublicKeySpec spec = new RSAPublicKeySpec(nBigInt, eBigInt);
				KeyFactory factory = KeyFactory.getInstance("RSA");
				return factory.generatePublic(spec);
			}
		}
		return null; // 못 찾으면 null 반환
	}

	// 애플 서버에서 키 목록을 받아와 캐시를 갱신하는 메서드
	private void fetchApplePublicKeys() {
		try {
			String response = restTemplate.getForObject(APPLE_PUBLIC_KEYS_URL, String.class);

			JsonNode root = objectMapper.readTree(response);
			JsonNode keysNode = root.get("keys");

			// 기존 캐시 비우고 새로 채우기
			cachedKeys.clear();
			if (keysNode.isArray()) {
				for (JsonNode key : keysNode) {
					cachedKeys.add(key);
				}
			}

			lastFetchTime = System.currentTimeMillis();
		} catch (Exception e) {
			throw new AuthException("애플 공개키 패치 실패: " + e.getMessage());
		}
	}
}