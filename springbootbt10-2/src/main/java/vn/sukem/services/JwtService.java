package vn.sukem.services;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import vn.sukem.exceptions.JwtExpiredException;
import vn.sukem.exceptions.JwtSignatureException;

import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
	@Value("${security.jwt.secret-key}")
	private String secretKey;

	@Value("${security.jwt.expiration-time}")
	private long jwtExpiration;

	public String extractUsername(String token) {
		return extractAllClaims(token).getSubject();
	}

	public String generateToken(UserDetails userDetails) {
		return generateToken(new HashMap<>(), userDetails);
	}

	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		return buildToken(extraClaims, userDetails, jwtExpiration);
	}

	public long getExpirationTime() {
		return jwtExpiration;
	}

	private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
		try {
			Date now = new Date();

			JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().subject(userDetails.getUsername()).issueTime(now)
					.expirationTime(new Date(now.getTime() + expiration));
			extraClaims.forEach(claims::claim);

			SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
			signedJWT.sign(new MACSigner(getSignInKey()));

			return signedJWT.serialize();
		} catch (JOSEException e) {
			throw new IllegalStateException("Cannot generate JWT: " + e.getMessage(), e);
		}
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return username.equals(userDetails.getUsername());
	}

	private JWTClaimsSet extractAllClaims(String token) {
		try {
			SignedJWT signedJWT = SignedJWT.parse(token);

			// 1. Kiểm tra chữ ký
			if (!signedJWT.verify(new MACVerifier(getSignInKey()))) {
				throw new JwtSignatureException("The JWT signature is invalid");
			}

			// 2. Kiểm tra hạn sử dụng
			JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
			Date expiration = claims.getExpirationTime();
			if (expiration == null || expiration.before(new Date())) {
				throw new JwtExpiredException("The JWT token has expired");
			}

			return claims;
		} catch (ParseException e) {
			throw new IllegalArgumentException("Invalid JWT string: " + e.getMessage(), e);
		} catch (JOSEException e) {
			throw new JwtSignatureException("Cannot verify JWT: " + e.getMessage());
		}
	}

	private byte[] getSignInKey() {
		return Base64.getDecoder().decode(secretKey);
	}
}