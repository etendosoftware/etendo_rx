package com.etendorx.das.mime;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CustomArgumentResolver implements HandlerMethodArgumentResolver {

  private final HttpMessageConverter<MyClass> converter1;
  private final HttpMessageConverter<MyClass> converter2;

  public CustomArgumentResolver(HttpMessageConverter<MyClass> converter1, HttpMessageConverter<MyClass> converter2) {
    this.converter1 = converter1;
    this.converter2 = converter2;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return MyClass.class.isAssignableFrom(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    String param = webRequest.getParameter("param");
    HttpInputMessage inputMessage = createInputMessage(webRequest);
    if ("value1".equals(param)) {
      return converter1.read(MyClass.class, inputMessage);
    } else {
      return converter2.read(MyClass.class, inputMessage);
    }
  }

  private HttpInputMessage createInputMessage(NativeWebRequest webRequest) {
    return null;
  }

  // implementa el método createInputMessage
}
