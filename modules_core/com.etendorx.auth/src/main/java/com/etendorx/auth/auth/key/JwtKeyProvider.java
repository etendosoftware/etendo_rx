package com.etendorx.auth.auth.key;

import com.etendorx.utils.auth.key.JwtKeyUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.PrivateKey;

@Component
public class JwtKeyProvider {

  /**
   * Private key generation
   * > openssl genrsa -out private.pem 2048
   * <p>
   * Public key generation
   * > openssl rsa -in private.pem -pubout -outform PEM -out public_key.pem
   * <p>
   * Private key generation with PCKS8 format (The one used to generate the token)
   * > openssl pkcs8 -topk8 -inform PEM -in private.pem -out private_key.pem -nocrypt
   */

  public static final String PRIVATE_KEY_LOCATION = "private.key.location";
  public static final String PRIVATE_KEY_ENV = "private.key.env";

  private PrivateKey privateKey;

  @Autowired
  private Environment env;

  @Value("${private-key}")
  private String privateKeyValue;

  @PostConstruct
  public void init() throws IOException {
    loadPrivateKey();
  }

  public PrivateKey getPrivateKey() {
    return this.privateKey;
  }

  private void loadPrivateKey() throws IOException {
    this.privateKey = JwtKeyUtils.readPrivateKey(privateKeyValue);
  }
}
