package com.matthewcasperson.onenotebackend.controllers;

import club.caliope.udc.DocumentConverter;
import club.caliope.udc.InputFormat;
import com.microsoft.graph.http.BaseCollectionPage;
import com.microsoft.graph.models.Notebook;
import com.microsoft.graph.models.OnenotePage;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.OnenotePageCollectionPage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OneNoteController {

  @Autowired
  GraphServiceClient<Request> client;

  @GetMapping("/notes")
  public List<String> getNotes() {
    return getNotebooks()
        .stream()
        .map(n -> n.displayName)
        .collect(Collectors.toList());
  }

  @GetMapping("/notes/{name}/html")
  public String getNoteHtml(@PathVariable("name") final String name) {
    return getPageHTML(name);
  }

  @GetMapping("/notes/{name}/markdown")
  public String getNoteMarkdown(@PathVariable("name") final String name) {
    final String content = getPageHTML(name);
    return convertContent(content);
  }

  private String getPageHTML(final String name) {
    return getNotebooks()
        .stream()
        // find the notebook that matches the supplied name
        .filter(n -> name.equals(n.displayName))
        // we only expect one notebook to match
        .findFirst()
        // get the notebook sections
        .map(notebook -> notebook.sections)
        // get the first page from the first section
        .map(sections -> getSectionPages(sections.getCurrentPage().get(0).id).get(0))
        // get the page id
        .map(page -> page.id)
        // get the content of the page
        .flatMap(this::getPageContent)
        // if any of the operations above returned null, return an error message
        .orElse("Could not load page content");
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
          .toFile(output.toFile(), "markdown_strict-raw_html")
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

  private List<Notebook> getNotebooks() {
    return Optional.ofNullable(client
            .me()
            .onenote()
            .notebooks()
            .buildRequest(new QueryOption("$expand", "sections"))
            .get())
        .map(BaseCollectionPage::getCurrentPage)
        .orElseGet(List::of);
  }

  private List<OnenotePage> getSectionPages(final String id) {
    return Optional.ofNullable(client
            .me()
            .onenote()
            .sections(id)
            .pages()
            .buildRequest()
            .get())
        .map(OnenotePageCollectionPage::getCurrentPage)
        .orElseGet(List::of);
  }

  private Optional<String> getPageContent(final String id) {
      return Optional.ofNullable(client
          .me()
          .onenote()
          .pages(id)
          .content()
          .buildRequest()
          .get())
          .map(s -> toString(s, null));
  }

  private String toString(final InputStream stream, final String defaultValue) {
    try (stream) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException e) {
      return defaultValue;
    }
  }
}
