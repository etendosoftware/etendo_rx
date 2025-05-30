package com.etendorx.utils.auth.key;

import com.etendorx.utils.auth.key.exceptions.JwtKeyException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utility class for handling JWT keys.
 * <p>
 * This class provides methods to read and validate JWT keys, as well as to parse JWT tokens and
 * extract claims.
 */
public class JwtKeyUtils {

  static final Logger logger = LoggerFactory.getLogger(JwtKeyUtils.class);

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

  private JwtKeyUtils() {
    throw new UnsupportedOperationException("Utility class");
  }

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

  /**
   * Validates a JWT token using the provided public key.
   *
   * @param publicKey The public key to verify the JWT
   * @param jwt       The JWT to validate
   * @return true if the token is valid, false otherwise
   */
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

  /**
   * Parses a JWT and returns the claims as a Map
   *
   * @param publicKey The public key to verify the JWT
   * @param jwt       The JWT to parse
   * @return A Map with the claims
   * @throws ParseException If the JWT is not valid
   * @throws JOSEException  If the JWT is not valid
   */
  public static Map<String, Object> getJwtClaims(PublicKey publicKey, String jwt) throws ParseException,
      JOSEException {
    SignedJWT signedJWT = SignedJWT.parse(jwt);
    ECDSAVerifier verifier = new ECDSAVerifier((ECPublicKey) publicKey);
    signedJWT.verify(verifier);
    JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
    return new HashMap<>(claimsSet.getClaims());
  }

  /**
   * Cleans the key string by removing headers and whitespace.
   *
   * @param originalKey The original key string
   * @param spec        The key specification (e.g., "PUBLIC" or "PRIVATE")
   * @return The cleaned key string
   */
  public static <T extends Key> T readKey(String originalKey, String spec,
      Function<String, EncodedKeySpec> keySpec,
      BiFunction<KeyFactory, EncodedKeySpec, T> keyGenerator) {
    String cleanKey = cleanKeyHeaders(originalKey, spec);
    try {
      return keyGenerator.apply(KeyFactory.getInstance("EC"), keySpec.apply(cleanKey));
    } catch (JwtKeyException ex) {
      logger.warn("Deprecated Public Key - Upgrade Core");
      return handleKeyException(keySpec, keyGenerator, cleanKey);
    } catch (NoSuchAlgorithmException e) {
      throw new JwtKeyException("Error reading the '" + spec + "' key.", e);
    }
  }

  /**
   * Handles the key exception by attempting to read the key using a different algorithm.
   *
   * @param keySpec       The key specification function
   * @param keyGenerator  The key generator function
   * @param cleanKey      The cleaned key string
   * @return The generated key
   */
  private static <T extends Key> T handleKeyException(Function<String, EncodedKeySpec> keySpec,
      BiFunction<KeyFactory, EncodedKeySpec, T> keyGenerator, String cleanKey) {
    try {
      return keyGenerator.apply(KeyFactory.getInstance("RSA"), keySpec.apply(cleanKey));
    } catch (NoSuchAlgorithmException e) {
      throw new JwtKeyException("Unsopported Algorithm - Use a EC Token", e);
    }
  }

  /**
   * Generates a {@link PKCS8EncodedKeySpec} from a key string.
   *
   * @param data The raw key string
   * @return The generated PKCS8EncodedKeySpec
   */
  public static EncodedKeySpec privateKeySpec(String data) {
    return new PKCS8EncodedKeySpec(Base64.getDecoder().decode(data));
  }

  /**
   * Generates a {@link PrivateKey} from a key specification.
   *
   * @param kf   The KeyFactory to use
   * @param spec The key specification
   * @return The generated PrivateKey
   */
  public static PrivateKey privateKeyGenerator(KeyFactory kf, EncodedKeySpec spec) {
    try {
      return kf.generatePrivate(spec);
    } catch (InvalidKeySpecException e) {
      throw new JwtKeyException("Something went wrong while reading the private key.", e);
    }
  }

  /**
   * Generates a {@link X509EncodedKeySpec} from a key string.
   *
   * @param data The raw key string
   * @return The generated X509EncodedKeySpec
   */
  public static EncodedKeySpec publicKeySpec(String data) {
    return new X509EncodedKeySpec(Base64.getDecoder().decode(data));
  }

  /**
   * Generates a {@link PublicKey} from a key specification.
   *
   * @param kf   The KeyFactory to use
   * @param spec The key specification
   * @return The generated PublicKey
   */
  public static PublicKey publicKeyGenerator(KeyFactory kf, EncodedKeySpec spec) {
    try {
      return kf.generatePublic(spec);
    } catch (InvalidKeySpecException e) {
      throw new JwtKeyException("Something went wrong while reading the public key.", e);
    }
  }

  /**
   * Cleans the key string by removing headers and whitespace.
   *
   * @param key    The original key string
   * @param header The header to remove (e.g., "PUBLIC" or "PRIVATE")
   * @return The cleaned key string
   */
  public static String cleanKeyHeaders(String key, String header) {
    key = key.replace("-----BEGIN " + header + " KEY-----", "");
    key = key.replace("-----END " + header + " KEY-----", "");
    return key.replaceAll("\\s+", "");
  }

  /**
   * Parses an unsigned JWT token and returns the claims as a Map.
   *
   * @param publicKey The public key to verify the JWT
   * @param token     The JWT token to parse
   * @return A Map with the claims
   * @throws ParseException If the JWT is not valid
   * @throws JOSEException  If the JWT is not valid
   */
  public static Map<String, Object> parseUnsignedToken(String publicKey, String token) throws ParseException,
      JOSEException {
    PublicKey pk = JwtKeyUtils.readPublicKey(publicKey);
    return JwtKeyUtils.getJwtClaims(pk, token);
  }

  /**
   * Parses a JWT token and returns the claims as a Map.
   *
   * @param publicKey The public key to verify the JWT
   * @param token     The JWT token to parse
   * @return A Map with the claims
   */
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

  /**
   * Validates that the token contains the required key values.
   *
   * @param tokenValuesMap The map of token values
   * @param keyValues      The list of required key values
   * @throws IllegalArgumentException if any required key value is missing
   */
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
