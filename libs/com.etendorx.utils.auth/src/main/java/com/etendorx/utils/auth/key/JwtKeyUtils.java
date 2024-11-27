package com.etendorx.utils.auth.key;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.etendorx.utils.auth.key.exceptions.JwtKeyException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
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
  public static final String ORG_ID = "ad_org_id";
  public static final String ROLE_ID = "ad_role_id";
  public static final String SERVICE_SEARCH_KEY = "search_key";
  public static final String SERVICE_ID = "service_id";
  public static final String CLASSIC_USER = "user";
  public static final String CLASSIC_CLIENT = "client";
  public static final String CLASSIC_ORGANIZATION = "organization";
  public static final String CLASSIC_ROLE = "role";

  /**
   * Generates a {@link PrivateKey} from a key String
   *
   * @param privateKeyStr The raw key String
   * @return {@link PrivateKey}
   */
  public static PrivateKey readPrivateKey(String privateKeyStr) {
    return readKey(privateKeyStr, "PRIVATE", JwtKeyUtils::privateKeySpec,
        JwtKeyUtils::privateKeyGenerator);
  }

  /**
   * Generates a {@link PublicKey} from a key String
   *
   * @param publicKeyStr The raw key String
   * @return {@link PublicKey}
   */
  public static PublicKey readPublicKey(String publicKeyStr) {
    return readKey(publicKeyStr, "PUBLIC", JwtKeyUtils::publicKeySpec,
        JwtKeyUtils::publicKeyGenerator);
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

  public static Map<String, Object> getJwtClaims(PublicKey publicKey, String jwt) {
    Algorithm algorithm = Algorithm.ECDSA256((java.security.interfaces.ECPublicKey) publicKey);

    DecodedJWT decodedJWT = JWT.require(algorithm)
        .build()
        .verify(jwt);

    Map<String, Object> claimsMap = new HashMap<>();
    decodedJWT.getClaims().forEach((key, claim) -> claimsMap.put(key, claim.as(Object.class)));

    return claimsMap;
  }

  public static <T extends Key> T readKey(String originalKey, String spec,
      Function<String, EncodedKeySpec> keySpec,
      BiFunction<KeyFactory, EncodedKeySpec, T> keyGenerator) {
    String cleanKey = cleanKeyHeaders(originalKey, spec);
    try {
      return keyGenerator.apply(KeyFactory.getInstance("EC"), keySpec.apply(cleanKey));
    } catch (JwtKeyException ex) {
      logger.warn("Deprecated Public Key - Upgrade Core");
      return handleKeyException(spec, keySpec, keyGenerator, cleanKey);
    } catch (NoSuchAlgorithmException e) {
      throw new JwtKeyException("Error reading the '" + spec + "' key.", e);
    }
  }

  private static <T extends Key> T handleKeyException(String spec, Function<String, EncodedKeySpec> keySpec,
      BiFunction<KeyFactory, EncodedKeySpec, T> keyGenerator, String cleanKey) {
    try {
      return keyGenerator.apply(KeyFactory.getInstance("RSA"), keySpec.apply(cleanKey));
    } catch (NoSuchAlgorithmException e) {
      throw new JwtKeyException("Unsopported Algorithm - Use a EC or RSA Token", e);
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
    } catch (InvalidKeySpecException e) {
      throw new JwtKeyException("Something went wrong while reading the public key.", e);
    }
  }

  public static String cleanKeyHeaders(String key, String header) {
    key = key.replace("-----BEGIN " + header + " KEY-----", "");
    key = key.replace("-----END " + header + " KEY-----", "");
    return key.replaceAll("\\s+", "");
  }

  public static Map<String, Object> parseUnsignedToken(String publicKey, String token) {
    PublicKey pk = JwtKeyUtils.readPublicKey(publicKey);
    return JwtKeyUtils.getJwtClaims(pk, token);
  }

  public static Map<String, Object> getTokenValues(String publicKey, String token) {
    try {
      var map = new HashMap<>(parseUnsignedToken(publicKey, token));
      if(!map.containsKey(USER_ID_CLAIM) && map.containsKey(CLASSIC_USER)) {
        map.put(USER_ID_CLAIM, map.get(CLASSIC_USER));
        map.remove(CLASSIC_USER);
      }
      if(!map.containsKey(CLIENT_ID_CLAIM) && map.containsKey(CLASSIC_CLIENT)) {
        map.put(CLIENT_ID_CLAIM, map.get(CLASSIC_CLIENT));
        map.remove(CLASSIC_CLIENT);
      }
      if(!map.containsKey(ORG_ID) && map.containsKey(CLASSIC_ORGANIZATION)) {
        map.put(ORG_ID, map.get(CLASSIC_ORGANIZATION));
        map.remove(CLASSIC_ORGANIZATION);
      }
      if(!map.containsKey(ROLE_ID) && map.containsKey(CLASSIC_ROLE)) {
        map.put(ROLE_ID, map.get(CLASSIC_ROLE));
        map.remove(CLASSIC_ROLE);
      }
      return map;
    } catch (Exception e) {
      logger.error("Error parsing the token '{}' - {}", token, e.getMessage());
      throw new IllegalArgumentException(e);
    }
  }

  public static void validateTokenValues(Map<String, Object> tokenValuesMap,
      List<String> keyValues) {
    for (String keyValue : keyValues) {
      if (!tokenValuesMap.containsKey(keyValue)) {
        throw new IllegalArgumentException(
            "The token is missing the required key value '" + keyValue + "'");
      }
    }
  }
}
