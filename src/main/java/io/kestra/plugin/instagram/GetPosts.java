package io.kestra.plugin.instagram;

import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import reactor.core.publisher.Flux;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Fetch Instagram posts using a hashtag", description = "Fetch posts from Instagram containing a specific hashtag")
@Plugin(examples = { @io.kestra.core.models.annotations.Example(title = "Fetch Instagram Posts", code = {
        "token: apify_api_*******************", "hashtag: blackfridaysale", "numberOfPosts: 1" }) })
public class GetPosts extends Task implements RunnableTask<GetPosts.Output> {
    private static final String APIFY_API_URL = "https://api.apify.com/v2/acts/apify~instagram-hashtag-scraper/run-sync-get-dataset-items";
    private static final int DEFAULT_MEMORY = 256;

    @Schema(title = "Apify Token", description = "Token from Apify Console. Get it at: https://console.apify.com/settings/integrations", requiredMode = Schema.RequiredMode.REQUIRED)
    @PluginProperty(dynamic = true)
    private String token;

    @Schema(title = "Hashtag", description = "Hashtag for which posts will be fetched")
    @PluginProperty(dynamic = true)
    @Builder.Default
    private String hashtag = "blackfridaysale";

    @Schema(title = "Number of Posts", description = "Number of posts that will be fetched from Instagram")
    @PluginProperty(dynamic = true)
    @Builder.Default
    private String numberOfPosts = "1";

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        // Prepare input parameters
        String inputToken = runContext.render(token);
        String inputNumberOfPosts = runContext.render(numberOfPosts);
        String inputHashtag = runContext.render(hashtag).replaceFirst("^#", "");
        File tempFile = runContext.workingDir().createTempFile().toFile();

        // Prepare HTTP client and object mapper
        HttpClient httpClient = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);

        // Construct request URL
        String requestUrl = String.format("%s?token=%s&maxItems=%s&memory=%d", APIFY_API_URL, inputToken,
                inputNumberOfPosts, DEFAULT_MEMORY);

        // Prepare request body
        String requestBody = String.format("{ \"hashtags\": [\"%s\"] }", inputHashtag);

        // Create HTTP request
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestUrl))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Send request and get response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Parse response
        PostsData[] postsObject = objectMapper.readValue(response.body(), PostsData[].class);

        // Extract post captions
        List<String> posts = new ArrayList<>();
        for (PostsData post : postsObject) {
            posts.add(post.caption);
        }

        try (Writer output = new BufferedWriter(new FileWriter(tempFile))) {

            output.write(objectMapper.writeValueAsString(posts));
            output.flush();
            logger.debug("tempFile: {}", tempFile);

            URI tempFileURI = runContext.storage().putFile(tempFile);

            logger.debug("tempFileURI: {}", tempFileURI);

            return Output.builder().uri(tempFileURI).build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "URI of a temporary result file containing an array of posts")
        private final URI uri;

        @Schema(title = "Date of Data")
        private final String currentDate;
    }
}