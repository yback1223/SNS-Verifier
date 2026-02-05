package back.yback.verifier;

import back.yback.common.AuthProvider;
import back.yback.dto.AuthUserDto;

public interface SocialVerifier {
	boolean support(AuthProvider provider);
	AuthUserDto verify(String token);
}
