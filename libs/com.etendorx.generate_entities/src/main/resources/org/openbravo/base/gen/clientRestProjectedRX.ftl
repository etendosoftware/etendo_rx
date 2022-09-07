package ${packageClientRestProjected};

import com.etendorx.clientrest.base.ClientRestBase;
import org.springframework.cloud.openfeign.FeignClient;
import com.etendorx.clientrest.base.FeignConfiguration;
import org.springframework.http.ResponseEntity;

@FeignClient(name="${feignClientName}", url = "${"$"}{das.url}/${entity.name}", configuration = FeignConfiguration.class)
public interface ${newClassName}ClientRest extends ClientRestBase<${locationModelProjectionClass}> {

}
