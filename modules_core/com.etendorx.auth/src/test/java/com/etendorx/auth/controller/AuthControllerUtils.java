package com.etendorx.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class AuthControllerUtils {

    public static String getUsernameNotFoundResponseBody(String port) {
        return "{\n" +
                "    \"_embedded\": {\n" +
                "        \"adUsers\": []\n" +
                "    },\n" +
                "    \"_links\": {\n" +
                "        \"self\": {\n" +
                "            \"href\": \"http://localhost:"+port+"/adUsers/search/searchByUsername?username=admins&page=0&size=20\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"page\": {\n" +
                "        \"size\": 20,\n" +
                "        \"totalElements\": 0,\n" +
                "        \"totalPages\": 0,\n" +
                "        \"number\": 0\n" +
                "    }\n" +
                "}";
    }

    public static String getSearchUserResponseBody(String port) {
        return "{\n" +
                "    \"_embedded\": {\n" +
                "        \"adUsers\": [\n" +
                "            {\n" +
                "                \"id\": \"100\",\n" +
                "                \"active\": true,\n" +
                "                \"creationDate\": \"2021-11-01T16:05:23.297+00:00\",\n" +
                "                \"updated\": \"2021-11-03T23:14:29.995+00:00\",\n" +
                "                \"name\": \"Admin\",\n" +
                "                \"description\": null,\n" +
                "                \"password\": \"1$H6E0K1Xx5nSb4h8y4YizSA$DozgBLpXFnxERX2++LhNVwC/w0dT8PObpkyKGpyuTtCWYMSfa+l6HfMk91PqhLzbYBr38JMILU4GA5FtAE6HoA==\",\n" +
                "                \"email\": null,\n" +
                "                \"processNow\": false,\n" +
                "                \"emailServerUsername\": \"info\",\n" +
                "                \"emailServerPassword\": null,\n" +
                "                \"position\": null,\n" +
                "                \"comments\": null,\n" +
                "                \"phone\": null,\n" +
                "                \"alternativePhone\": null,\n" +
                "                \"fax\": null,\n" +
                "                \"lastContactDate\": \"2021-11-02T15:09:17.000+00:00\",\n" +
                "                \"lastContactResult\": \"I/35\",\n" +
                "                \"birthday\": null,\n" +
                "                \"firstName\": \"Admin\",\n" +
                "                \"lastName\": null,\n" +
                "                \"username\": \"admin\",\n" +
                "                \"locked\": false,\n" +
                "                \"grantPortalAccess\": false,\n" +
                "                \"lastPasswordUpdate\": \"2016-01-20T14:26:57.000+00:00\",\n" +
                "                \"isPasswordExpired\": false,\n" +
                "                \"commercialauth\": false,\n" +
                "                \"viasms\": false,\n" +
                "                \"viaemail\": false,\n" +
                "                \"_links\": {\n" +
                "                    \"self\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100\"\n" +
                "                    },\n" +
                "                    \"adUser\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100{?projection}\",\n" +
                "                        \"templated\": true\n" +
                "                    },\n" +
                "                    \"defaultOrganization\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/defaultOrganization\"\n" +
                "                    },\n" +
                "                    \"defaultWarehouse\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/defaultWarehouse\"\n" +
                "                    },\n" +
                "                    \"smfswsDefaultWsRole\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/smfswsDefaultWsRole\"\n" +
                "                    },\n" +
                "                    \"partnerAddress\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/partnerAddress\"\n" +
                "                    },\n" +
                "                    \"trxOrganization\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/trxOrganization\"\n" +
                "                    },\n" +
                "                    \"organization\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/organization\"\n" +
                "                    },\n" +
                "                    \"image\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/image\"\n" +
                "                    },\n" +
                "                    \"defaultClient\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/defaultClient\"\n" +
                "                    },\n" +
                "                    \"businessPartner\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/businessPartner\"\n" +
                "                    },\n" +
                "                    \"greeting\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/greeting\"\n" +
                "                    },\n" +
                "                    \"supervisor\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/supervisor{?projection}\",\n" +
                "                        \"templated\": true\n" +
                "                    },\n" +
                "                    \"defaultLanguage\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/defaultLanguage\"\n" +
                "                    },\n" +
                "                    \"defaultRole\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/defaultRole\"\n" +
                "                    },\n" +
                "                    \"client\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/client\"\n" +
                "                    },\n" +
                "                    \"updatedBy\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/updatedBy{?projection}\",\n" +
                "                        \"templated\": true\n" +
                "                    },\n" +
                "                    \"createdBy\": {\n" +
                "                        \"href\": \"http://localhost:"+port+"/adUsers/100/createdBy{?projection}\",\n" +
                "                        \"templated\": true\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"_links\": {\n" +
                "        \"self\": {\n" +
                "            \"href\": \"http://localhost:"+port+"/adUsers/search/searchByUsername?username=admin&page=0&size=20\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"page\": {\n" +
                "        \"size\": 20,\n" +
                "        \"totalElements\": 1,\n" +
                "        \"totalPages\": 1,\n" +
                "        \"number\": 0\n" +
                "    }\n" +
                "}";
    }

    public static Map<String, Object> parseUrl(String url) {
        String[] urlSplit = url.split(":");
        Map<String, Object> map = new HashMap<>();
        map.put("host",  urlSplit[0]);
        map.put("port", Integer.parseInt(urlSplit[1]));
        return map;
    }

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
