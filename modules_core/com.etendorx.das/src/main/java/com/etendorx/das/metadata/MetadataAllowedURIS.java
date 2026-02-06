package com.etendorx.das.metadata;

import com.etendorx.utils.auth.key.context.AllowedURIS;
import org.springframework.stereotype.Component;

/**
 * Allows unauthenticated access to the metadata diagnostic endpoints.
 * TODO: Remove or restrict after Phase 1 validation.
 */
@Component
public class MetadataAllowedURIS implements AllowedURIS {
    @Override
    public boolean isAllowed(String requestURI) {
        return requestURI.startsWith("/api/metadata/");
    }
}
