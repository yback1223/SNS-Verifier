package org.example.verifier;

import org.example.common.AuthProvider;
import org.example.dto.AuthUserDto;

public interface SocialVerifier {
	boolean support(AuthProvider provider);
	AuthUserDto verify(String token);
}
