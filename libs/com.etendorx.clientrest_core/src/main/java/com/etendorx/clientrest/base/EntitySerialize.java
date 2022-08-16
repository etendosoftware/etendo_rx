package com.etendorx.clientrest.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.Link;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class EntitySerialize extends JsonSerializer<RepresentationWithId<?>> {

  @Override public void serialize(RepresentationWithId<?> value, JsonGenerator gen,
    SerializerProvider serializers) throws IOException {
    if (Objects.requireNonNull(value).getLink("self").isPresent()) {
      Optional<Link> link = Objects.requireNonNull(value).getLink("self");
      if (link.isPresent()) {
        gen.writeObject(link.get().getHref());
      }
    }
  }
}
