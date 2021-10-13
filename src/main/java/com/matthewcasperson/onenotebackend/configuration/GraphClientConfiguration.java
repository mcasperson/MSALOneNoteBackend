package com.matthewcasperson.onenotebackend.configuration;

import com.azure.spring.autoconfigure.aad.AADAuthenticationProperties;
import com.matthewcasperson.onenotebackend.providers.OboAuthenticationProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import java.util.Set;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphClientConfiguration {
  @Autowired
  AADAuthenticationProperties azureAd;

  @Bean
  public GraphServiceClient<Request> getClient() {
    return GraphServiceClient.builder()
        .authenticationProvider(new OboAuthenticationProvider(
            Set.of("https://graph.microsoft.com/Notes.Read.All"),
            azureAd.getTenantId(),
            azureAd.getClientId(),
            azureAd.getClientSecret()))
        .buildClient();
  }
}
