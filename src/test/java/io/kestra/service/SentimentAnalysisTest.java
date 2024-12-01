package io.kestra.service;

import com.google.common.collect.ImmutableMap;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.List;

/**
 * This test will only test the main task, this allow you to send any input
 * parameters to your task and test the returning behaviour easily.
 */
@MicronautTest
class SentimentAnalysisTest {
    @Test
    void run() throws Exception {
        SentimentAnalysisApiClient sentimentAnalysisApiClient = new SentimentAnalysisApiClient(
                "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");
        List<SentimentAnalysisApiClient.SentimentResult> sentimentResult = sentimentAnalysisApiClient
                .analyzeSentiment(List.of("This product is very good", "I hate this product"));
        System.out.println(sentimentResult);
        assertThat(sentimentResult.size(), is(2));
    }
}