package com.instaclone.infrastructure.security.jwt.resource;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JtiBlacklistValidator implements OAuth2TokenValidator<Jwt> {

    private final JtiBlacklist jtiBlacklist;

    public JtiBlacklistValidator(JtiBlacklist jtiBlacklist) {
        this.jtiBlacklist = jtiBlacklist;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String jti = token.getId();
        if (jti != null && jtiBlacklist.isBlacklisted(jti)) {
            OAuth2Error error = new OAuth2Error("token_revoked", "The access token has been revoked", null);
            return OAuth2TokenValidatorResult.failure(error);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
