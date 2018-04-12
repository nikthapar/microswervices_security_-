package com.offershoffer.uaaserver.feignproxy;

import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.offershoffer.uaaserver.model.RegisterInfo;

@FeignClient(value = "registration-service", url = "")
@RibbonClient(name = "registration-service")
public interface RegisterProxyRepo {

	@PostMapping("/registration")
	public String newUser(@RequestBody RegisterInfo obj);

}
