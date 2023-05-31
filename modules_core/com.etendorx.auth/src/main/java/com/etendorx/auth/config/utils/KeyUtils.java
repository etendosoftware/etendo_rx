package com.etendorx.auth.config.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;


import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Date;
import java.time.ZonedDateTime;

import static com.etendorx.utils.auth.key.JwtKeyUtils.generateUserClaims;


public class KeyUtils {
    private static final String ISS = "EtendoRX Auth";
    private static final String SEARCH_KEY = "searchKey";
    public static final String SERVICE_ID = "serviceId";
    public static final String USER_ID = "100";
    public static final String CLIENT_ID = "0";
    public static final String ORG_ID = "0";
    public static final String ROLE_ID = "0";

    public static String generateToken(String privateKey) throws Exception {
        Claims claims = generateUserClaims(USER_ID, CLIENT_ID, ORG_ID,
                ROLE_ID, SEARCH_KEY, SERVICE_ID);
        return Jwts.builder()
                .setIssuer(ISS)
                .setIssuedAt(Date.from(ZonedDateTime.now().toInstant()))
                .addClaims(claims)
                .signWith(SignatureAlgorithm.RS256, convertStringToPrivateKey(privateKey))
                .compact();
    }

    public static PrivateKey convertStringToPrivateKey(String privateKeyString) throws Exception {
        // Eliminar los encabezados y pies del formato PEM
        privateKeyString = privateKeyString.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] privateKeyBytes = java.util.Base64.getDecoder().decode(privateKeyString);

        // Crear una instancia de KeyFactory con el algoritmo RSA
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        // Crear la especificación de la clave privada PKCS8EncodedKeySpec
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);

        // Generar la clave privada utilizando KeyFactory
        return keyFactory.generatePrivate(keySpec);
    }

}
