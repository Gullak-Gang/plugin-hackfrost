package io.kestra.plugin.instagram;

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
class GetPostsTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of(ImmutableMap.of(
                "token", "apify_api_p9bfRqx74jUne8bfLV9vyN5gfcuP351aYjXC",
                "hashtag", "diwali_sale",
                "number_of_posts", "1"));

        GetPosts task = GetPosts.builder()
                .token("{{ token }}")
                .hashtag("{{ hashtag }}")
                .build();

        GetPosts.Output runOutput = task.run(runContext);
        assertThat(runOutput.getPosts().size(), is(1));
        assertThat(runOutput.getCurrentDate(), is(LocalDate.now().toString()));
    }
}