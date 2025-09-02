package com.bonoya.platform.iam.application.internal.outboundservices.tokens;

public interface TokenService {
    String generateToken(String username);
    boolean validateToken(String token);
    String getUsernameFromToken(String token);
}
