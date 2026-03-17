package site.ng_archive.ecom_order.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.operation.preprocess.OperationRequestPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationResponsePreprocessor;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.restdocs.snippet.Attributes.Attribute;
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

import java.util.function.Consumer;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;

@TestConfiguration
public class RestDocsConfig {

    @Value("${spring.profiles.active:local}")
    private String profile;

    public OperationRequestPreprocessor getRequestPreprocessor() {
        return switch (profile) {
            case "local" -> Preprocessors.preprocessRequest(
                    Preprocessors.prettyPrint(),
                    modifyUris().scheme("http").host("localhost")
            );
            default -> Preprocessors.preprocessRequest(
                    Preprocessors.prettyPrint(),
                    modifyUris().scheme("https").host("prod-host").removePort()
            );
        };
    }

    public OperationResponsePreprocessor getResponsePreprocessor() {
        return Preprocessors.preprocessResponse(Preprocessors.prettyPrint());
    }

    @Bean
    public Consumer<EntityExchangeResult<byte[]>> restDocs() {
        return WebTestClientRestDocumentation.document(
                "{class-name}/{method-name}",
                getRequestPreprocessor(),
                getResponsePreprocessor()
        );
    }

    public static final Attribute field(
            final String key,
            final String value){
        return new Attribute(key,value);
    }

}
