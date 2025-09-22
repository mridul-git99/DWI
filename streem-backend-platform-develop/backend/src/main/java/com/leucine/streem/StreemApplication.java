package com.leucine.streem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leucine.streem.config.JaasRestInterceptor;
import com.leucine.streem.config.JaasServiceProperty;
import com.leucine.streem.exception.JaasServiceExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

//TODO Check usage of wrapper class for Dtos
@SpringBootApplication
@EnableAsync(proxyTargetClass = true)
@EnableMongoRepositories
public class StreemApplication {
  @Autowired
  JaasServiceProperty jaasServiceProperty;


  public static String applicationPropertiesPath = null;

  public static void main(String[] args) {
    for(String arg : args) {
      if(arg.startsWith("--spring.config.location")) {
        StreemApplication.applicationPropertiesPath = arg.split("=")[1].replace("file:", "");
      }
    }
    SpringApplication.run(StreemApplication.class, args);
  }

  @Primary
  @Bean(name = "jaasRestTemplate")
  public RestTemplate jaasRestTemplate() {
    RestTemplate template = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    List interceptors = template.getInterceptors();
    if (null == interceptors) {
      template.setInterceptors(Collections.singletonList(new JaasRestInterceptor(jaasServiceProperty)));
    } else {
      interceptors.add(new JaasRestInterceptor(jaasServiceProperty));
      template.setInterceptors(interceptors);
    }
//    template.setErrorHandler(new JaasRestTemplateResponseErrorHandler());
    template.setErrorHandler(new JaasServiceExceptionHandler());
    return template;
  }

  @Bean(name = "authenticationFilterRestTemplate")
  public RestTemplate authenticationFilterRestTemplate() {
    RestTemplate template = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    List interceptors = template.getInterceptors();
    if (null == interceptors) {
      template.setInterceptors(Collections.singletonList(new JaasRestInterceptor(jaasServiceProperty)));
    } else {
      interceptors.add(new JaasRestInterceptor(jaasServiceProperty));
      template.setInterceptors(interceptors);
    }
    return template;
  }

  @Bean
  public ObjectMapper getObjectMapper() {
    return new ObjectMapper();
  }

  // TODO this is a workaround find alternate approach
  @Bean("threadPoolTaskExecutor")
  public TaskExecutor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(10);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setThreadNamePrefix("Async-");
    // Explicitly calling initialize() during bean creation ensures that the thread pool is set up and ready for use as soon as the application context starts up.
    // If there are any issues, they will be highlighted immediately, making it easier to detect and fix them during the startup process rather than during regular application operation.
    // If initialize is not called, thread pool will get initialized when the first task is submitted for execution.
    executor.initialize();

    return new DelegatingSecurityContextAsyncTaskExecutor(executor);

  }

  @PostConstruct
  public void init(){
    // Setting Spring Boot SetTimeZone
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }
}
