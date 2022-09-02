package com.etendorx.das.test;

import com.etendorx.das.grpcrepo.ADUserGrpcService;
import com.etendorx.das.grpcrepo.PricingAdjustmentGrpcService;
import com.etendorx.test.grpc.*;
import com.google.protobuf.Timestamp;
import io.grpc.internal.testing.StreamRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@ComponentScan("com.etendorx.das.grpcrepo")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19092")
@AutoConfigureMockMvc
public class GrpcTest {
  @Autowired
  private ADUserGrpcService adUserGrpcService;
  @Autowired
  private PricingAdjustmentGrpcService pricingAdjustmentGrpcService;

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
  }

  @Container
  public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
    DockerImageName.parse("futit/etendodata:latest").asCompatibleSubstituteFor("postgres")
  )
    .withPassword("syspass")
    .withUsername("postgres")
    .withEnv("PGDATA", "/postgres")
    .withDatabaseName("etendo")
    .waitingFor(new HostPortWaitStrategy());

  public static Stream<Arguments> validRequestParams() {
    return Stream.of(
      // Undefined username
      Arguments.of("100", "admin", true),
      Arguments.of("167450A5BB284AB29C4FEF039E98C963", "F&BESUser", false),
      Arguments.of("26EF171A1D75485083D276D49AAACD45", "F&BESRSUser", false),
      Arguments.of("5A79667096964E83A6985D549C988275", "F&BUSUser", false),
      Arguments.of("D249DE7A14FB4F77BC056A3738A63477", "F&BUSECUser", false),
      Arguments.of("E12DC7B3FF8C4F64924A98195223B1F8", "F&BUser", false)
    );
  }

  @ParameterizedTest
  @MethodSource("validRequestParams")
  void testGetUser(String id, String username, boolean active) throws Exception {

    var search = ADUser_searchByUsernameSearch.newBuilder();
    search.setUsername(username);
    search.setActive(active);
    StreamRecorder<ADUserList> observer = StreamRecorder.create();

    adUserGrpcService.searchByUsername(search.build(), observer);
    if (!observer.awaitCompletion(5, TimeUnit.SECONDS)) {
      fail("The call did not terminate in time");
    }

    assertNull(observer.getError());
    List<ADUserList> results = observer.getValues();
    assertEquals(1, results.size());
    ADUserList response = results.get(0);
    var adUser = response.getAduser(0);
    assertEquals(ADUser.newBuilder()
      .setId(id)
      .setUsername(username)
      .build(), adUser);

  }

  public static Stream<Arguments> validDiscount() {
    return Stream.of(
      // Undefined username
      Arguments.of("32F782F545FC4F4EBCDE08041DFDD457", "Price adjustment (Discount amount)", 1230775200L),
      Arguments.of("7B7121F368AB4EE6A29586C2B16A70D9", "Price adjustment (Discount %)", 1230775200L),
      Arguments.of("AD3C7B494FF947C4896217BC9830C6EB", "Price adjustment (Fixed Price)", 1230775200L)
      );
  }

  @ParameterizedTest
  @MethodSource("validDiscount")
  void testTimestampField(String id, String name, long timestamp) throws Exception {
    var ts = Timestamp.newBuilder();
    ts.setSeconds(timestamp);
    var search = PricingAdjustment_searchOfferSearch.newBuilder();
    search.setId(id);
    StreamRecorder<PricingAdjustmentList> observer = StreamRecorder.create();

    pricingAdjustmentGrpcService.searchOffer(search.build(), observer);
    if (!observer.awaitCompletion(5, TimeUnit.SECONDS)) {
      fail("The call did not terminate in time");
    }

    assertNull(observer.getError());
    List<PricingAdjustmentList> results = observer.getValues();
    assertEquals(1, results.size());
    PricingAdjustmentList response = results.get(0);
    var pricingAdjustment = response.getPricingadjustment(0);
    assertEquals(PricingAdjustment.newBuilder()
      .setId(id)
      .setName(name)
      .setStartingDate(ts)
      .build(), pricingAdjustment);

  }
}
