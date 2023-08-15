package com.etendorx.das.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpringContext implements ApplicationContextAware {
  private static ApplicationContext context;

  /**
   * Returns the Spring managed bean instance of the given class type if it exists.
   * Returns null otherwise.
   *
   * @param beanClass
   */
  public static <T extends Object> T getBean(Class<T> beanClass) {
    log.info("Context: {}", context);
    return context.getBean(beanClass);
  }

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    SpringContext.context = context;
  }
}
