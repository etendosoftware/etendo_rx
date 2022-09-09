package com.etendorx.das.test;

import com.etendorx.das.grpcrepo.ADUserGrpcService;
import com.etendorx.das.grpcrepo.OrderGrpcService;
import com.etendorx.das.grpcrepo.PricingAdjustmentGrpcService;
import com.etendorx.test.grpc.*;
import com.google.protobuf.Timestamp;
import io.grpc.internal.testing.StreamRecorder;
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

  @Autowired
  private OrderGrpcService orderGrpcService;

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

  public static Stream<Arguments> validBusinessPartnerCategoryName() {
    return Stream.of(
      Arguments.of("100", null, null),
      Arguments.of("167450A5BB284AB29C4FEF039E98C963", null, null),
      Arguments.of("26EF171A1D75485083D276D49AAACD45", null, null),
      Arguments.of("5A79667096964E83A6985D549C988275", null, null),
      Arguments.of("D249DE7A14FB4F77BC056A3738A63477", null, null),
      Arguments.of("E12DC7B3FF8C4F64924A98195223B1F8", null, null),
      Arguments.of("20C5D31133D949F0BD25412DD1069612", "Fruit & Bio is Life, Inc.", "Supplier"),
      Arguments.of("2748452130E84FF0B1A8292D88570F8F", "Moon Light Restaurants, Co.", "Customer - Tier 2"),
      Arguments.of("33FE57CFE5BE4774B9B9EDFD8E27BCAE", "Bebidas Alegres, S.L.", "Supplier"),
      Arguments.of("50A34002FDA34FC58F1319E25EDA4E3A", "La Fruta es la Vida, S.L.", "Supplier"),
      Arguments.of("545A04EE5EF94E9B967536140226793F", "Healthy Food Supermarkets, Co.", "Customer - Tier 1"),
      Arguments.of("6628F632D484407CBCBD8E71C123A263", "Be Soft Drinker, Inc.", "Supplier"),
      Arguments.of("6822CC074A064323B639A7087ED14859", "Refrescos Naturales, S.A.", "Supplier"),
      Arguments.of("6A3D3D6A808C455EAF1DAB48058FDBF4", "Restaurantes Luna Llena, S.A.", "Customer - Tier 2"),
      Arguments.of("6B1E8F0CA1524850AB3A7F9AE475A16F", "Alimentos y Supermercados, S.A", "Customer - Tier 1"),
      Arguments.of("81FE8AF4E1EF4488B0D92143942C79C3", "Sleep Well Hotels, Co.", "Customer - Tier 3"),
      Arguments.of("8537B1F5669E423ABA79F1F57B1E4222", "John Smith", "Employee"),
      Arguments.of("A6EA3469E33544D184F836D97F274E0A", "Luca Simone", "Employee"),
      Arguments.of("BFEF159DC6BF4178913C9E38FB706155", "Happy Drinks, Inc.", "Supplier"),
      Arguments.of("C3503BEFB3CB4848A674284A656163B9", "Javier Martín", "Employee"),
      Arguments.of("CA5D537DFD014F15BFFBA0DB81E1A379", "Hoteles Buenas Noches, S.A.", "Customer - Tier 3"),
      Arguments.of("CADCDC3549FB4201B5F24E4C03AD2349", "Juan López", "Employee"),
      Arguments.of("4028E6C72959682B01295F40C1D702E6", "Vendor A", "Vendor"),
      Arguments.of("4028E6C72959682B01295F40C30F02EA", "Vendor B", "Vendor"),
      Arguments.of("4028E6C72959682B01295F40C43802EF", "Customer A", "Customer"),
      Arguments.of("4028E6C72959682B01295F40C52302F3", "Customer B", "Customer"),
      Arguments.of("4028E6C72959682B01295F40C63C02F8", "Employee A", "Employee"),
      Arguments.of("4028E6C72959682B01295F40C72602FC", "Employee B", "Employee"),
      Arguments.of("4028E6C72959682B01295F40C84F0301", "Salesman A", "Salesman"),
      Arguments.of("4028E6C72959682B01295F40C93A0305", "Salesman B", "Salesman"),
      Arguments.of("4028E6C72959682B01295F40CA62030A", "Creditor", "Financial Accounts")
    );
  }

  @ParameterizedTest
  @MethodSource("validBusinessPartnerCategoryName")
  void testBusinessPartnerCategoryName(String id, String businessPartnerName, String businessPartnerCategoryName) throws Exception {

    var search = ADUser_searchByIdSearch.newBuilder();
    search.setId(id);
    StreamRecorder<ADUserList> observer = StreamRecorder.create();

    adUserGrpcService.searchById(search.build(), observer);
    if (!observer.awaitCompletion(5, TimeUnit.SECONDS)) {
      fail("The call did not terminate in time");
    }

    assertNull(observer.getError());
    List<ADUserList> results = observer.getValues();
    assertEquals(1, results.size());
    ADUserList response = results.get(0);
    var adUser = response.getAduser(0);

    var adUserBuilder= ADUser.newBuilder()
      .setId(id);
    if(businessPartnerName != null)
      adUserBuilder.setBusinessPartnerName(businessPartnerName);
    if(businessPartnerCategoryName != null)
      adUserBuilder.setBusinessPartnerCategoryName(businessPartnerCategoryName);
    //
    if(adUser.getUsername().compareTo("") != 0) {
      adUserBuilder.setUsername(adUser.getUsername());
    }
    var adUserAssert = adUserBuilder.build();

    assertEquals(adUserAssert, adUser);

  }

  public static Stream<Arguments> validScheduledDeliveryDate() {
    return Stream.of(
      Arguments.of("012B01C662A240BE98CEAB30ADC62299", 1321326000),
      Arguments.of("014125CAF3774A29B8087831FBE1E82C", 1348542000),
      Arguments.of("00247EB519C941DDA87A7F5630421924", 1313377200),
      Arguments.of("00E9A9664A814E96B884A2B80810754E", 1366254000),
      Arguments.of("0175087588CB4C04A63F6DE8D7D7FC74", 1313377200),
      Arguments.of("035DFB0924964B4894CAB8E07AF7514F", 1346209200),
      Arguments.of("037F63F41D3045228E0304DDC234D179", 1327114800),
      Arguments.of("00E9A9664A814E96B884A2B80810754E", 1366254000),
      Arguments.of("00247EB519C941DDA87A7F5630421924", 1313377200),
      Arguments.of("014125CAF3774A29B8087831FBE1E82C", 1348542000),
      Arguments.of("043B6945860346F980545408A7186AD1", 1343358000)
    );
  }

  @ParameterizedTest
  @MethodSource("validScheduledDeliveryDate")
  void testScheduledDeliveryDate(String id, long scheduledDeliveryDate) throws Exception {

    var search = Order_searchByIdSearch.newBuilder();
    search.setId(id);
    StreamRecorder<OrderList> observer = StreamRecorder.create();

    orderGrpcService.searchById(search.build(), observer);
    if (!observer.awaitCompletion(5, TimeUnit.SECONDS)) {
      fail("The call did not terminate in time");
    }

    assertNull(observer.getError());
    List<OrderList> results = observer.getValues();
    assertEquals(1, results.size());
    OrderList response = results.get(0);
    var order = response.getOrder(0);
    var ts = Timestamp.newBuilder();
    ts.setSeconds(scheduledDeliveryDate);

    var orderBuilder= Order.newBuilder()
      .setId(id);
    orderBuilder.setDatePromised(ts.build());
    var orderAssert = orderBuilder.build();
    assertEquals(orderAssert, order);
  }
}
