import com.etendorx.das.integration.PropertyMetadataTest;
import com.etendorx.das.test.DefaultFiltersTest;
import com.etendorx.das.test.DisableEnableTriggersTest;
import com.etendorx.das.test.FieldMappingRestCallTest;
import com.etendorx.das.test.RepositoryTest;
import com.etendorx.das.test.RestCallTest;
import com.etendorx.das.test.eventhandlertest.test.AnnotationTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@Suite
@SuiteDisplayName("Etendo RX Spring Boot Tests Suite")
@SelectClasses({ DefaultFiltersTest.class, DisableEnableTriggersTest.class,
    FieldMappingRestCallTest.class, PropertyMetadataTest.class,
    RepositoryTest.class, RestCallTest.class,
    AnnotationTests.class
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19090")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
public class EtendoRXSpringBootTestsSuite {
}
