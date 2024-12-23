package com.etendorx.utils.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Utility class for managing paths within JSON strings.
 */
public class PathManagementUtils {

  private static final Logger logger = LoggerFactory.getLogger(PathManagementUtils.class);

  public PathManagementUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Replaces placeholders in the URL template with values extracted from the JSON string.
   *
   * @param urlTemplate the URL template containing placeholders in the format {{$.jsonPath}}
   * @param jsonString the JSON string from which values will be extracted
   * @return the URL with placeholders replaced by corresponding values from the JSON string
   */
  public static final String getURL(String urlTemplate, String jsonString) {
    Pattern pattern = Pattern.compile("\\{\\{(\\$.+?)}}");
    Matcher matcher = pattern.matcher(urlTemplate);
    StringBuffer resultUrl = new StringBuffer();
    while (matcher.find()) {
      String jsonPath = matcher.group(1);
      logger.info("Extracting value from JSON using path: " + jsonPath);
      try {
        Object value = JsonPath.read(jsonString, jsonPath);
        matcher.appendReplacement(resultUrl, value.toString());
      } catch (PathNotFoundException e) {
        matcher.appendReplacement(resultUrl, "");
        logger.error("Path not found in JSON: " + jsonPath, e);
      }
    }
    matcher.appendTail(resultUrl);
    logger.info("URL with placeholders replaced by values: " + resultUrl.toString());
    return resultUrl.toString();
  }
}