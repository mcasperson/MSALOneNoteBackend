package com.matthewcasperson.onenotebackend.controllers;

import club.caliope.udc.DocumentConverter;
import club.caliope.udc.InputFormat;
import club.caliope.udc.OutputFormat;
import com.azure.core.annotation.PathParam;
import com.microsoft.graph.models.Notebook;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageRequest;
import com.microsoft.graph.requests.NotebookCollectionPage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OneNoteController {

  @Autowired
  GraphServiceClient<Request> client;

  @GetMapping("/notes/{name}/markdown")
  public String getNotes(@PathParam("name") final String name) {
    final List<Notebook> notebooks = getNotebooks();
    final String url = notebooks.stream()
        .filter(n -> n.displayName.equals(name))
        .findFirst()
        .map(n -> n.sections.getCurrentPage().get(0))
        .map(s -> s.pages.getCurrentPage().get(0))
        .map(p -> p.contentUrl)
        .orElse("");

    final String content = getPageContent(url);
    return convertContent(content);
  }

  private String convertContent(final String html) {
    Path input = null;
    Path output = null;

    try {
      input = Files.createTempFile(null, ".html");
      output = Files.createTempFile(null, ".md");

      Files.write(input, html.getBytes());

      new DocumentConverter()
          .fromFile(input.toFile(), InputFormat.HTML)
          .toFile(output.toFile(), OutputFormat.MARKDOWN)
          .convert();

      return Files.readString(output);
    } catch (final IOException e) {
      // silently ignore
    } finally {
      try {
        if (input != null) {
          Files.delete(input);
        }
        if (output != null) {
          Files.delete(output);
        }
      } catch (final Exception ex) {
        // silently ignore
      }
    }

    return "There was an error converting the file";
  }

  private String getPageContent(final String url) {
    return new MessageRequest(url, client, null)
        .get().body.content;
  }

  private List<Notebook> getNotebooks() {
    final NotebookCollectionPage notebooks = client
        .me()
        .onenote()
        .notebooks()
        .buildRequest()
        .get();

    // assume we will only ever have one page of results.
    return notebooks != null ? notebooks.getCurrentPage() : List.of();
  }
}
