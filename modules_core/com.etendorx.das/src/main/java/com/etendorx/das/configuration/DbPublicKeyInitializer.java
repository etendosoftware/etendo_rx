package com.etendorx.das.configuration;

import com.etendorx.utils.auth.key.context.FilterContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Reads the public key from smfsws_config table at startup and injects it
 * into FilterContext. This enables JWT validation when running without
 * the config server (e.g., local profile).
 */
@Component
@Slf4j
public class DbPublicKeyInitializer {

    private final DataSource dataSource;
    private final FilterContext filterContext;

    public DbPublicKeyInitializer(DataSource dataSource, FilterContext filterContext) {
        this.dataSource = dataSource;
        this.filterContext = filterContext;
    }

    @PostConstruct
    public void init() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT privatekey FROM smfsws_config LIMIT 1");
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String rawValue = rs.getString(1);
                String publicKey = extractPublicKey(rawValue);
                if (publicKey != null) {
                    Field field = FilterContext.class.getDeclaredField("publicKey");
                    field.setAccessible(true);
                    field.set(filterContext, publicKey);
                    log.info("Public key loaded from smfsws_config and injected into FilterContext");
                } else {
                    log.warn("Could not extract public-key from smfsws_config.privatekey");
                }
            } else {
                log.warn("No rows found in smfsws_config table");
            }
        } catch (Exception e) {
            log.warn("Could not load public key from DB: {}", e.getMessage());
        }
    }

    private String extractPublicKey(String rawValue) {
        if (rawValue == null) return null;
        // Try parsing as JSON ({"private-key":"...","public-key":"..."})
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(rawValue);
            JsonNode publicKeyNode = node.get("public-key");
            if (publicKeyNode != null) {
                return publicKeyNode.asText();
            }
        } catch (Exception e) {
            log.debug("privatekey column is not JSON, trying as raw key");
        }
        // If it's already a raw PEM key, return as-is
        if (rawValue.contains("BEGIN PUBLIC KEY")) {
            return rawValue;
        }
        return null;
    }
}
