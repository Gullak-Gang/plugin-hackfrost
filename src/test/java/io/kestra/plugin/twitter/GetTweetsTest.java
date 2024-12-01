package io.kestra.plugin.twitter;

import com.google.common.collect.ImmutableMap;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

/**
 * This test will only test the main task, this allow you to send any input
 * parameters to your task and test the returning behaviour easily.
 */
@MicronautTest
class GetTweetsTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of(ImmutableMap.of("variable",
                "https://gullak-seven.vercel.app"));

        GetTweets task = GetTweets.builder()
                .url("{{ variable }}")
                .build();

        GetTweets.Output runOutput = task.run(runContext);
        assertThat(runOutput.getPosts().size(), is(10));
        assertThat(runOutput.getCurrentDate(), is(LocalDate.now().toString()));
    }
}