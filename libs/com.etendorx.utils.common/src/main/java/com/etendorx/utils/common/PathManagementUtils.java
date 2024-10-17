package com.etendorx.utils.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
public class PathManagementUtils {

  final static Logger logger = LoggerFactory.getLogger(PathManagementUtils.class);

  public static final String getURL(String urlTemplate, String jsonString) {
    Pattern pattern = Pattern.compile("\\{\\{(\\$.+?)}}");
    Matcher matcher = pattern.matcher(urlTemplate);
    StringBuffer resultUrl = new StringBuffer();

    while (matcher.find()) {
      String jsonPath = matcher.group(1);
      try {
        Object value = JsonPath.read(jsonString, jsonPath);
        matcher.appendReplacement(resultUrl, value.toString());
      } catch (PathNotFoundException e) {
        matcher.appendReplacement(resultUrl, "");
      }
    }
    matcher.appendTail(resultUrl);
    return resultUrl.toString();
  }
}
