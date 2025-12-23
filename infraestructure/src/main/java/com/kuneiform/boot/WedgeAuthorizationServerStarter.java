package com.kuneiform.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.kuneiform")
public class WedgeAuthorizationServerStarter {
  public static void main(String[] args) {
    SpringApplication.run(WedgeAuthorizationServerStarter.class, args);
  }
}
