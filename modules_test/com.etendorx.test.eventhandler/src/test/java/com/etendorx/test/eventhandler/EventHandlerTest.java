package com.etendorx.test.eventhandler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Objects;

import org.hibernate.event.spi.PreUpdateEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openbravo.model.ad.access.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.etendorx.das.EtendorxDasApplication;
import com.etendorx.entities.jparepo.ADUserRepository;
import com.etendorx.test.eventhandler.component.EventHandlerUser;

@EntityScan(basePackages = { "com.etendorx.test.eventhandler","com.etendorx.das.*","com.etendorx.entities.*" })
@ComponentScan(basePackages = { "com.etendorx.das.*","com.etendorx.entities.*"})
@EnableJpaRepositories(basePackages = "com.etendorx.entities.*")
@DataJpaTest
@AutoConfigureTestDatabase
//@SpringBootTest
public class EventHandlerTest {

  @Autowired
  ADUserRepository adUserRepository;

  @SpyBean
  private EventHandlerUser component;

  @Test
  void test() {
    User user = adUserRepository.findById("100").orElse(null);
    if (user != null) {
      user.setLastName("Test eventHandler");
      adUserRepository.save(user);
      verify(component).handlePreInsert(eq(user), any(PreUpdateEvent.class));
    }
    assertNotNull(user);
  }

}
