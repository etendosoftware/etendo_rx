package com.etendorx.das.scan;

import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.domain.EntityScan({
<#list packages as package>
  "${package}"<#if !package?is_last>, </#if>
</#list>
})
public class EntityScan {
}
