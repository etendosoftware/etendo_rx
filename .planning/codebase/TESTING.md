# Testing Patterns

**Analysis Date:** 2026-02-05

## Test Framework

**Runner:**
- JUnit 5 (Jupiter) - configured in `build.gradle`: `useJUnitPlatform()`
- JUnit Platform Suite 1.8.1 for test suites
- Spring Boot Test 3.1.4 for integration testing

**Assertion Library:**
- JUnit 5 Assertions: `org.junit.jupiter.api.Assertions`
  - Static imports: `assertEquals()`, `assertNotNull()`, `assertThrows()`, `assertTrue()`
- Mockito for mocking dependencies
- BDDMockito for Given-When-Then style assertions

**Run Commands:**
```bash
gradle test                    # Run all tests
gradle :modules_core:com.etendorx.das:test  # Run module tests
gradle test --watch           # Watch mode (continuous)
gradle jacocoRootReport        # Generate coverage report
gradle test -i                 # Info/debug logging
```

## Test File Organization

**Location:**
- **Unit tests:** `src/test/java/com/etendorx/das/unit/` directory
  - Example: `MappingUtilsImplTest.java`, `BindedRestControllerTest.java`, `BaseDTORepositoryDefaultTests.java`
- **Integration/Spring Boot tests:** `src/test/java/com/etendorx/das/test/` directory
  - Example: `RepositoryTest.java`, `DefaultFiltersTest.java`, `DisableEnableTriggersTest.java`
- **Test projections:** `src/test/java/org/openbravo/model/*/` directories
  - Example: `UserTestProjection.java`, `OrderTestProjection.java`, `ProductJMTestProjection.java`
- **Event handler tests:** `src/test/java/com/etendorx/das/test/eventhandlertest/` with subdirectories for domain, repository, component, test
- **Test suite files:** Root `src/test/java/` directory
  - `EtendoRXUnitTestsSuite.java` - aggregates unit tests
  - `EtendoRXSpringBootTestsSuite.java` - aggregates integration tests

**Naming:**
- Test classes: `[ClassName]Test.java` or `[ClassName]Tests.java`
- Test methods: `test[Scenario]()` or `[methodName]Should[ExpectedBehavior]()`
- Suite classes: `[ModuleName]TestSuite.java` or `[Category]TestsSuite.java`
- Projection test classes: `[Entity]TestProjection.java`

**Structure:**
```
src/test/java/
├── EtendoRXUnitTestsSuite.java
├── EtendoRXSpringBootTestsSuite.java
├── com/etendorx/das/
│   ├── unit/
│   │   ├── MappingUtilsImplTest.java
│   │   ├── BindedRestControllerTest.java
│   │   └── JsonPathConverterBaseTests.java
│   └── test/
│       ├── RepositoryTest.java
│       ├── DefaultFiltersTest.java
│       ├── DisableEnableTriggersTest.java
│       └── eventhandlertest/
│           ├── domain/
│           ├── repository/
│           ├── component/
│           └── test/
└── org/openbravo/model/
    ├── ad/access/UserTestProjection.java
    └── common/order/OrderTestProjection.java
```

## Test Structure

**Suite Organization:**
```java
// From EtendoRXUnitTestsSuite.java
@Suite
@SuiteDisplayName("Etendo RX Unit Tests Suite")
@SelectClasses({
    JsonPathConverterBaseTests.class,
    JsonPathEntityRetrieverBaseTests.class,
    MappingUtilsImplTest.class,
    JsonPathEntityRetrieverDefaultTest.class
})
public class EtendoRXUnitTestsSuite {
}
```

**Patterns:**

Unit Tests (from `MappingUtilsImplTest.java`):
```java
public class MappingUtilsImplTest {
  @Mock
  private ETRX_Constant_ValueRepository constantValueRepository;

  private MappingUtilsImpl mappingUtils;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mappingUtils = new MappingUtilsImpl(constantValueRepository);
    // AppContext setup
    var uc = new UserContext();
    uc.setDateFormat("yyyy-MM-dd");
    uc.setTimeZone("UTC");
    AppContext.setCurrentUser(uc);
  }

  @Test
  void testHandleBaseObjectWithSerializableObject() {
    // Given - Arrange
    BaseSerializableObject serializableObject = mock(BaseSerializableObject.class);
    when(serializableObject.get_identifier()).thenReturn("123");

    // When - Act
    Object result = mappingUtils.handleBaseObject(serializableObject);

    // Then - Assert
    assertEquals("123", result);
  }
}
```

Integration Tests (from `DefaultFiltersTest.java`):
```java
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "grpc.server.port=19090"
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
public class DefaultFiltersTest {
  @Test
  void testAddFiltersGetMethod() {
    // Arrange
    boolean isActive = true;

    // Act
    String result = DefaultFilters.addFilters(
        SELECT_QUERY, USER_ID_123, CLIENT_ID_456,
        ROLE_ID_101112, isActive, REST_METHOD_GET
    );

    // Assert
    String expected = "SELECT * FROM table t1_0 WHERE ...";
    assertEquals(expected, result);
  }
}
```

Spring Boot Test (from `RepositoryTest.java`):
```java
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "grpc.server.port=19091",
        "public-key=" + RepositoryTest.publicKey,
        "scan.basePackage=com.etendorx.subapp.product.javamap",
        "data-rest.enabled=true"
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
public class RepositoryTest {
  @LocalServerPort
  private int port;

  @Autowired
  private HttpServletRequest httpServletRequest;

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void whenReadUser() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter(FilterContext.NO_ACTIVE_FILTER_PARAMETER, FilterContext.TRUE);
    request.setMethod("GET");
    setUserContextFromToken(userContext, publicKey, null, TOKEN, request);
    AppContext.setCurrentUser(userContext);
    var allUsers = userRepository.findAll();
    assert allUsers.iterator().hasNext();
  }
}
```

## Mocking

**Framework:** Mockito 3.x (included with Spring Boot Test)

**Patterns:**
```java
// Field-based mocking
@Mock
private ETRX_Constant_ValueRepository constantValueRepository;

@Mock
private JsonPathConverter<CarDTOWrite> converter;

// Setup in @BeforeEach
@BeforeEach
void setUp() {
  MockitoAnnotations.openMocks(this);  // Initialize mocks
  mappingUtils = new MappingUtilsImpl(constantValueRepository);
}

// Inline mocking
BaseSerializableObject serializableObject = mock(BaseSerializableObject.class);

// Stubbing
when(serializableObject.get_identifier()).thenReturn("123");
when(constantValueRepository.findById(id)).thenReturn(Optional.of(constantValue));

// BDD-style mocking
given(repository.findAll(any())).willReturn(expectedPage);
given(repository.findById(anyString())).willReturn(expectedEntity);
```

**What to Mock:**
- External service dependencies: repositories, REST clients, message queues
- Third-party library integrations
- System resources that are slow or have side effects: databases (use H2 test database instead), file systems, network calls
- Complex dependencies that are not under test

**What NOT to Mock:**
- The class under test (instantiate it directly with mocked dependencies)
- Value objects and DTOs (create real instances)
- Spring components that should be tested with integration tests (use `@SpringBootTest`)
- Business logic in service classes (test real behavior, not mocked behavior)
- Lightweight utilities and helpers

## Fixtures and Factories

**Test Data:**
```java
// From DefaultFiltersTest.java - Constants as test data
private static final String SELECT_QUERY = "SELECT * FROM table t1_0 LIMIT 10";
private static final String UPDATE_QUERY = "UPDATE t1_0 SET column = 'value' WHERE t1_0.table_id = 1";
private static final String DELETE_QUERY = "DELETE FROM table t1_0 WHERE t1_0.table_id = 1";
public static final String USER_ID_123 = "123";
public static final String CLIENT_ID_456 = "456";
public static final String ROLE_ID_101112 = "101112";

// From RepositoryTest.java - JWT token and public key fixtures
private static final String TOKEN = "eyJhbGciOiJFUzI1NiJ9...";
public static final String publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE9Om8W9iL...";

// From BindedRestControllerTest.java - Mock inner classes for testing
static class Car implements BaseSerializableObject {
  public String getId() { return "id"; }

  @Override
  public String get_identifier() { return "_id"; }
}

static class CarDTORead implements BaseDTOModel {
  public String getId() { return "id"; }

  @Override
  public void setId(String id) { /* do nothing */ }
}
```

**Location:**
- Test constants defined as `private static final` or `public static final` in test classes
- Reusable test data kept in separate test configuration classes or fixtures
- Fixture classes use `@TestConfiguration` or `static class` pattern for component definitions
- Test data builders and factories for complex objects not yet detected; use direct instantiation

## Coverage

**Requirements:**
- JaCoCo configured in `build.gradle` with version 0.8.10
- Root-level `jacocoRootReport` task aggregates coverage from all subprojects
- Coverage reports generated in HTML and XML format
- Code coverage not explicitly enforced (no threshold configured)
- Coverage reports available at build outputs

**View Coverage:**
```bash
# Generate full coverage report
gradle jacocoRootReport

# Open HTML report
open build/reports/jacoco/jacocoRootReport/html/index.html

# View coverage per subproject
gradle :modules_core:com.etendorx.das:test jacocoTestReport
```

## Test Types

**Unit Tests:**
- Scope: Single class or method in isolation
- Location: `src/test/java/com/etendorx/das/unit/`
- Dependencies: All external collaborators mocked
- Database: No database access; in-memory stubs for repositories
- Speed: Fast, typically < 100ms per test
- Examples: `MappingUtilsImplTest`, `BindedRestControllerTest`, `BaseDTORepositoryDefaultTests`
- Setup: MockitoAnnotations.openMocks() to initialize mock fields
- Assertions: Direct assertion of return values and method invocations

**Integration Tests:**
- Scope: Multiple components working together; Spring context loaded
- Location: `src/test/java/com/etendorx/das/test/`
- Dependencies: Real Spring beans; some external services mocked
- Database: H2 in-memory test database configured: `<h2>1.4.200</h2>`
- Speed: Slower, typically 1-5 seconds per test suite
- Examples: `RepositoryTest`, `DefaultFiltersTest`, `DisableEnableTriggersTest`
- Setup: `@SpringBootTest` with `AutoConfigureTestDatabase.Replace.NONE`
- Assertions: Verify full request/response flow, database state changes

**E2E Tests:**
- Framework: Not used; integration tests with `TestRestTemplate` and `MockMvc` provide E2E coverage
- Alternative: `RepositoryTest` uses `TestRestTemplate` for HTTP testing with real Spring server (RANDOM_PORT)
- Parametrized E2E tests: `@ParameterizedTest` with `@CsvFileSource` to test multiple endpoints

## Common Patterns

**Async Testing:**
Not extensively used; traditional synchronous testing with `TestRestTemplate` and `MockMvc`:
```java
// From RepositoryTest.java
@Autowired
private TestRestTemplate testRestTemplate;

@Autowired
private MockMvc mockMvc;

// Making HTTP requests
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
```

**Error Testing:**
```java
// From BindedRestControllerTest.java - Testing error scenarios
@Test
void getShouldReturnNotFound() {
  // Mock setup
  given(repository.findById(anyString())).willReturn(null);

  // Execute
  ResponseEntity<CarDTORead> response = controller.get("someId");

  // Assert
  assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
}

// Exception testing
@Test
void testAddFiltersWithUnknownMethod() {
  assertThrows(IllegalArgumentException.class, () -> {
    DefaultFilters.addFilters(SELECT_QUERY, USER_ID, CLIENT_ID, ROLE_ID, true, "UNKNOWN");
  });
}
```

**Parametrized Testing:**
```java
// From RepositoryTest.java - CSV-based parametrization
@ParameterizedTest
@CsvFileSource(resources = "/urlData.csv", numLinesToSkip = 1)
public void queryIsOkWhenDefaultFiltersIsApplyWithCsvParameter(String parametrizedUrl)
    throws IOException, InterruptedException {
  // Test logic for each CSV row
}
```

**Test Lifecycle:**
- `@BeforeEach` - Run before each test method; initialize mocks and fixtures
- `@BeforeClass` / `@BeforeAll` - Run once before all tests in class (not extensively used)
- No `@AfterEach` or `@After` patterns detected; mocks cleaned up by MockitoAnnotations.openMocks()
- Spring context reused across tests in same `@SpringBootTest` class

**Assertion Patterns:**
```java
// Direct assertion
assertEquals(expected, result);
assertEquals(HttpStatus.OK, response.getStatusCode());
assertEquals(expectedPage, result);

// Existence checks
assertNotNull(result);
assert allUsers.iterator().hasNext();

// Boolean checks
assertTrue(Files.exists(path));

// Exception assertions
assertThrows(IllegalArgumentException.class, () -> { ... });

// Collection assertions
assert userList.getSize() == 1;
assert userList.getContent().get(0) != null;
```

---

*Testing analysis: 2026-02-05*
