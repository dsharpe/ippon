package com.ippon.poc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;

import com.ippon.poc.hostname.HostNameProperties;
import com.ippon.poc.hostname.HostNameRouteLocator;

@EnableZuulProxy
@SpringBootApplication
@EnableConfigurationProperties({ HostNameProperties.class })
public class GatewayApplication {

  @Autowired
  protected ZuulProperties zuulProperties;
  
  @Autowired
  protected HostNameProperties hostNameProperties;

  @Autowired
  protected ServerProperties server;

  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }

  @Bean
  public RouteLocator routeLocator() {
	return new HostNameRouteLocator(this.server.getServletPrefix(), this.zuulProperties, this.hostNameProperties);
  }
}
