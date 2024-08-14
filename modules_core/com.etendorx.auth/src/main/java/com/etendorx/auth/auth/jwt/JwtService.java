package com.etendorx.auth.auth.jwt;

import com.etendorx.auth.auth.key.JwtKeyProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.sql.Date;
import java.time.ZonedDateTime;

@Component
public class JwtService {

  private static final String ISS = "EtendoRX Auth";
  public static final String INVALID_TOKEN = "Invalid token.";

  @Autowired
  JwtKeyProvider jwtKeyProvider;

  public JwtResponse generateJwtToken(Claims claims) {
    PrivateKey privateKey = jwtKeyProvider.getPrivateKey();
    String token = Jwts.builder()
        .setIssuer(ISS)
        .setIssuedAt(Date.from(ZonedDateTime.now().toInstant()))
        .addClaims(claims)
        .signWith(SignatureAlgorithm.RS256, privateKey)
        .compact();
    return new JwtResponse(token);
  }

}
