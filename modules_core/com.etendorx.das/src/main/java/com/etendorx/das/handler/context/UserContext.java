package com.etendorx.das.handler.context;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserContext {

    private String userId;

    private String clientId;

    private String organizationId;

}
