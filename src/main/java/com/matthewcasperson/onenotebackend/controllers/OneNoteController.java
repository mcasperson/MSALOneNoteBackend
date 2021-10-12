package com.matthewcasperson.onenotebackend.controllers;

import com.azure.spring.autoconfigure.aad.AADAuthenticationProperties;
import com.matthewcasperson.onenotebackend.providers.OboAuthenticationProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OneNoteController {

  @Autowired
  AADAuthenticationProperties azureAd;

  @GetMapping("/me")
  public User getNotes() {
    return GraphServiceClient.builder()
        .authenticationProvider(new OboAuthenticationProvider(
            Set.of("https://graph.microsoft.com/user.read"),
            azureAd.getTenantId(),
            azureAd.getClientId(),
            azureAd.getClientSecret()))
        .buildClient()
        .me()
        .buildRequest()
        .get();
  }
}
