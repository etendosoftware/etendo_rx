package com.etendorx.das;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@RestController
public class ViewController {
  @GetMapping(value = "/sws/view", produces="application/json")
  public String view() {
    return getFileContent("productMetadata.json");
  }

  @GetMapping(value = "/sws/session", produces="application/json")
  public String session() {
    return getFileContent("session.json");
  }

  @NotNull
  private String getFileContent(String fileName) {
    ClassLoader classLoader = getClass().getClassLoader();

    String content = "";
    try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
        InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader)) {

      String line;
      while ((line = reader.readLine()) != null) {
        content += line;
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
    return content;
  }
}
