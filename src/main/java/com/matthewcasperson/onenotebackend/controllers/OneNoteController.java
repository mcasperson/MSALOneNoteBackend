package com.matthewcasperson.onenotebackend.controllers;

import club.caliope.udc.DocumentConverter;
import club.caliope.udc.InputFormat;
import club.caliope.udc.OutputFormat;
import com.azure.core.annotation.PathParam;
import com.microsoft.graph.http.BaseCollectionPage;
import com.microsoft.graph.models.Notebook;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageRequest;
import com.microsoft.graph.requests.NotebookCollectionPage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OneNoteController {

  @Autowired
  GraphServiceClient<Request> client;

  @GetMapping("/notes/{name}/markdown")
  public String getNotes(@PathVariable("name") final String name) {
    final List<Notebook> notebooks = getNotebooks();
    final String content = notebooks.stream()
        .filter(n -> name.equals(n.displayName))
        .findFirst()
        .map(notebook -> notebook.sections)
        .map(sections -> sections.getCurrentPage().get(0))
        .map(section -> section.pages)
        .map(pages -> pages.getCurrentPage().get(0))
        .map(page -> page.contentUrl)
        .flatMap(this::getPageContent)
        .orElse("Failed to read notebook page");

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

  private Optional<String> getPageContent(final String url) {
    return Optional.ofNullable(new MessageRequest(url, client, null).get())
        .map(r -> r.body)
        .map(b -> b.content);
  }

  private List<Notebook> getNotebooks() {
    return Optional.ofNullable(client
        .me()
        .onenote()
        .notebooks()
        .buildRequest()
        .get())
        .map(BaseCollectionPage::getCurrentPage)
        .orElseGet(List::of);
  }
}
