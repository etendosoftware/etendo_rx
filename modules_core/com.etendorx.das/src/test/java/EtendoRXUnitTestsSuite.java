import com.etendorx.das.unit.*;
import com.etendorx.entities.mapper.lib.JsonPathEntityRetrieverBaseTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Etendo RX Unit Tests Suite")
@SelectClasses({
    BaseDTORepositoryDefaultTests.class,
    JsonPathConverterBaseTests.class,
    JsonPathEntityRetrieverBaseTests.class,
    MappingUtilsImplTest.class,
    BindedRestControllerTest.class,
    JsonPathEntityRetrieverDefaultTest.class
})
public class EtendoRXUnitTestsSuite {
}
