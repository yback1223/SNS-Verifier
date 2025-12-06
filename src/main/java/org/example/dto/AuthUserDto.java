package org.example.dto;

import org.example.common.AuthProvider;

import java.util.Map;

public record AuthUserDto (
	String oAuthId,
	String email,
	String name,
	String pictureUrl,
	AuthProvider provider,
	Map<String, Object> rawAttributes
) {}
