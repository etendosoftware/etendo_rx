package com.etendorx.auth.feign;

import lombok.Data;

@Data
public class UserModel {
    private String id;

    private Boolean active;

    private String clientId;

    private String organizationId;

    private String defaultClientId;

    private String defaultOrganizationId;

    private String username;

    private String password;
}
