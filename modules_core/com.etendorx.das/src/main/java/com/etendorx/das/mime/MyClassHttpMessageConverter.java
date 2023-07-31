package com.etendorx.das.mime;

import java.io.IOException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class MyClassHttpMessageConverter extends AbstractHttpMessageConverter<MyClass> {

  public MyClassHttpMessageConverter() {
    super(new MediaType("application", "vnd.mycompany.myclass+json"));
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return MyClass.class == clazz;
  }

  @Override
  protected MyClass readInternal(Class<? extends MyClass> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
    // implementa la lógica para convertir el cuerpo de la solicitud HTTP en un objeto MyClass
    MyClass myClass = new MyClass();
    return myClass;
  }

  @Override
  protected void writeInternal(MyClass myClass, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
    // implementa la lógica para convertir un objeto MyClass en el cuerpo de la respuesta HTTP
    outputMessage.getBody().write("Hello World".getBytes());
  }
}
