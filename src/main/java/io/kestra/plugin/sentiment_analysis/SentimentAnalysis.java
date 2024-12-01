package io.kestra.plugin.sentiment_analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.service.SentimentAnalysisApiClient;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import java.io.*;

import org.slf4j.Logger;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Perform Sentiment Analysis", description = "Use this plugin to Perform a Sentiment Analysis on an array of strings containing various posts")
@Plugin(examples = {@io.kestra.core.models.annotations.Example(title = "Fetch Instagram Posts", code = {
    "posts: [\"post1\", \"post2\", \"post3\"]",})})
public class SentimentAnalysis extends Task implements RunnableTask<SentimentAnalysis.Output> {

    @NonNull
    @Schema(title = "Posts File", description = "A file containing a list of posts for which analysis will be performed")
    @PluginProperty(dynamic = true)
    private String posts;

    @NonNull
    @Schema(title = "Groq API Key", description = "API Key from https://console.groq.com/keys", requiredMode = Schema.RequiredMode.REQUIRED)
    @PluginProperty(dynamic = true)
    private String api_key;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        // Prepare Object Mapper
        ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Prepare input parameters
        URI inputPosts = new URI(runContext.render(posts));
        String inputApiKey = runContext.render(api_key);
        File tempFile = runContext.workingDir().createTempFile().toFile();

        try (
            Reader reader = new BufferedReader(
                new InputStreamReader(runContext.storage().getFile(inputPosts), StandardCharsets.UTF_8),
                FileSerde.BUFFER_SIZE);
            Writer output = new BufferedWriter(new FileWriter(tempFile))
        ) {
            List<String> postList = om.readValue(reader, new TypeReference<>() {
            });
            SentimentAnalysisApiClient sentimentAnalysisApiClient = new SentimentAnalysisApiClient(
                inputApiKey);
            List<SentimentAnalysisApiClient.SentimentResult> sentimentResults = sentimentAnalysisApiClient
                .analyzeSentiment(postList);

            logger.debug("sentimentResults: {}", sentimentResults);

            output.write(om.writeValueAsString(sentimentResults));
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
        @Schema(title = "URI of a temporary file containing Sentiment Analysis result")
        private URI uri;
    }
}