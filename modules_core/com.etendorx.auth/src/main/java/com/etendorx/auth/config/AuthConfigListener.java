package com.etendorx.auth.config;

import io.jsonwebtoken.JwtBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;

import static com.etendorx.auth.config.utils.KeyUtils.generateToken;

@Slf4j
@Component
public class AuthConfigListener {
    private final AuthConfig authConfig;

    @Autowired
    public AuthConfigListener(AuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) throws Exception {
        String token = authConfig.getToken();
        String privateKey = authConfig.getPrivateKey();
        if (StringUtils.isEmpty(token)) {
            String tokenFromKeys = generateToken(privateKey);
            String message = "\n\n**********************************************";
            message+="\nPopulate the auth.yaml file with the following property: \n token: {}";
            message+="\n**********************************************\n";
            log.info(message, tokenFromKeys);

            System.exit(1);
        }
    }
}

