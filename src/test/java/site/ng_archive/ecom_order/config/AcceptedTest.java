package site.ng_archive.ecom_order.config;

import com.epages.restdocs.apispec.*;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.Constraint;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.snippet.Attributes.key;

@Import(RestDocsConfig.class)
@ExtendWith({RestDocumentationExtension.class})
@AutoConfigureRestDocs
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class AcceptedTest {

    @Autowired
    protected RestDocsConfig restDocsConfig;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp(ApplicationContext applicationContext, RestDocumentationContextProvider provider) {
        this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
                .configureClient()
                .filter(WebTestClientRestDocumentation.documentationConfiguration(provider))
                .build();

        RestAssuredWebTestClient.webTestClient(webTestClient);
    }

    protected static ResourceSnippetParametersBuilder info() {
        return ResourceSnippetParameters.builder();
    }

    protected Consumer<EntityExchangeResult<byte[]>> document(
            ResourceSnippetParametersBuilder info,
            Snippet... snippets) {

        return WebTestClientRestDocumentationWrapper.document(
                "{class-name}/{method-name}",
                restDocsConfig.getRequestPreprocessor(),
                restDocsConfig.getResponsePreprocessor(),
                mergeSnippets(snippets, getResource(info))
        );
    }

    protected static FieldDescriptor field(Class<?> clazz, String path, String description) {
        List<String> constraints = new ConstraintDescriptions(clazz).descriptionsForProperty(path);
        String combinedDescription = description;

        if (!constraints.isEmpty()) {
            combinedDescription += " (" + String.join(", ", constraints) + ")";
        }

        return fieldWithPath(path)
            .description(combinedDescription)
            .attributes(
                key("validationConstraints").value(
                    constraints.stream()
                        .map(c -> new Constraint(c, Collections.emptyMap()))
                        .toList()
                )
            );
    }

    private static ResourceSnippet getResource(ResourceSnippetParametersBuilder info) {
        return ResourceDocumentation.resource(info.build());
    }

    private static Snippet[] mergeSnippets(Snippet[] snippets, ResourceSnippet resourceSnippet) {
        Snippet[] allSnippets = new Snippet[snippets.length + 1];
        allSnippets[0] = resourceSnippet;
        System.arraycopy(snippets, 0, allSnippets, 1, snippets.length);
        return allSnippets;
    }

}
