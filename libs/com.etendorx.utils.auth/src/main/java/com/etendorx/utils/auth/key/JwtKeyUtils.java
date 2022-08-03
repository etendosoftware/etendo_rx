package com.etendorx.utils.auth.key;

import com.etendorx.utils.auth.key.exceptions.JwtKeyException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class JwtKeyUtils {

    final static Logger logger = LoggerFactory.getLogger(JwtKeyUtils.class);

    public static final String USER_ID_CLAIM = "ad_user_id";
    public static final String CLIENT_ID_CLAIM = "ad_client_id";
    public static final String ORG_ID_CLAIM = "ad_org_id";

    /**
     * Generates a {@link PrivateKey} from a key String
     * @param privateKeyStr The raw key String
     * @return {@link PrivateKey}
     */
    public static PrivateKey readPrivateKey(String privateKeyStr) {
        return readKey(
                privateKeyStr,
                "PRIVATE",
                JwtKeyUtils::privateKeySpec,
                JwtKeyUtils::privateKeyGenerator
        );
    }

    /**
     * Generates a {@link PublicKey} from a key String
     * @param publicKeyStr The raw key String
     * @return {@link PublicKey}
     */
    public static PublicKey readPublicKey(String publicKeyStr) {
        return readKey(
                publicKeyStr,
                "PUBLIC",
                JwtKeyUtils::publicKeySpec,
                JwtKeyUtils::publicKeyGenerator
        );
    }

    public static boolean isValidToken(PublicKey publicKey, String jwt) {
        try {
            getJwtClaims(publicKey, jwt);
            return true;
        } catch (JwtException e) {
            logger.warn("Invalid JSON WEB TOKEN '{}' - {}", jwt, e.getMessage());
        } catch (Exception e) {
            logger.warn("Something went wrong validating the token '{}' - {}", jwt, e.getMessage());
        }
        return false;
    }

    public static Claims getJwtClaims(PublicKey publicKey, String jwt) {
        return Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jwt).getBody();
    }

    public static <T extends Key> T readKey(String originalKey, String spec, Function<String, EncodedKeySpec> keySpec, BiFunction<KeyFactory, EncodedKeySpec, T> keyGenerator) {
        try {
            String cleanKey = cleanKeyHeaders(originalKey, spec);
            return keyGenerator.apply(KeyFactory.getInstance("RSA"), keySpec.apply(cleanKey));
        } catch (NoSuchAlgorithmException e) {
            throw new JwtKeyException("Something went wrong while reading the '"+spec+"' key.", e);
        }
    }

    public static EncodedKeySpec privateKeySpec(String data) {
        return new PKCS8EncodedKeySpec(Base64.getDecoder().decode(data));
    }

    public static PrivateKey privateKeyGenerator(KeyFactory kf, EncodedKeySpec spec) {
        try {
            return kf.generatePrivate(spec);
        } catch (InvalidKeySpecException e) {
            throw new JwtKeyException("Something went wrong while reading the private key.", e);
        }
    }

    public static EncodedKeySpec publicKeySpec(String data) {
        return new X509EncodedKeySpec(Base64.getDecoder().decode(data));
    }

    public static PublicKey publicKeyGenerator(KeyFactory kf, EncodedKeySpec spec) {
        try {
            return kf.generatePublic(spec);
        } catch(InvalidKeySpecException e) {
            throw new JwtKeyException("Something went wrong while reading the public key.", e);
        }
    }

    public static String cleanKeyHeaders(String key, String header) {
        key = key.replace("-----BEGIN " + header + " KEY-----", "");
        key = key.replace("-----END " + header + " KEY-----", "");
        return key.replaceAll("\\s+", "");
    }

    public static Claims parseUnsignedToken(String token) {
        String[] splitToken = token.split("\\.");
        return (Claims) Jwts.parser().parse(splitToken[0] + "." + splitToken[1] + ".").getBody();
    }

    public static String generateJwtToken(PrivateKey privateKey, Claims claims, String iss) {
        return Jwts.builder()
                .setIssuer(iss)
                .setIssuedAt(Date.from(ZonedDateTime.now().toInstant()))
                .addClaims(claims)
                .signWith(SignatureAlgorithm.RS256, privateKey)
                .compact();
    }

    public static Map<String, Object> getTokenValues(String token) {
        try {
            return new HashMap<>(parseUnsignedToken(token));
        } catch (Exception e) {
            logger.error("Error parsing the token '{}' - {}", token, e.getMessage());
            throw new IllegalArgumentException(e);
        }
    }

    public static void validateTokenValues(Map<String, Object> tokenValuesMap, List<String> keyValues) {
        for (String keyValue : keyValues) {
            if (!tokenValuesMap.containsKey(keyValue)) {
                throw new IllegalArgumentException("The token is missing the required key value '" + keyValue + "'");
            }
        }
    }

    public static Claims generateUserClaims(String userId, String clientId, String orgId) {
        Claims claims = new DefaultClaims();
        claims.put(USER_ID_CLAIM, userId);
        claims.put(CLIENT_ID_CLAIM, clientId);
        claims.put(ORG_ID_CLAIM, orgId);
        return claims;
    }

}
