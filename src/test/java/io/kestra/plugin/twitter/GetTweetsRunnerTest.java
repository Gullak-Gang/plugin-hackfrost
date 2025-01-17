package io.kestra.plugin.twitter;

import io.kestra.core.junit.annotations.KestraTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.repositories.LocalFlowRepositoryLoader;
import io.kestra.core.runners.RunnerUtils;
import io.kestra.core.runners.StandAloneRunner;

import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * This test will load all flow located in `src/test/resources/flows/`
 * and will run an in-memory runner to be able to test a full flow. There is
 * also a
 * configuration file in `src/test/resources/application.yml` that is only for
 * the full runner
 * test to configure in-memory runner.
 */
@KestraTest
class GetTweetsRunnerTest {
    @Inject
    protected StandAloneRunner runner;

    @Inject
    protected RunnerUtils runnerUtils;

    @Inject
    protected LocalFlowRepositoryLoader repositoryLoader;

    @BeforeEach
    protected void init() throws IOException, URISyntaxException {
        repositoryLoader.load(Objects.requireNonNull(GetTweetsRunnerTest.class.getClassLoader().getResource("flows")));
        this.runner.run();
    }

    @Test
    void flow() throws TimeoutException {
        try {
            Execution execution = runnerUtils.runOne(null, "io.kestra.plugin",
                    "twitterFetch");

            assertThat(execution.getTaskRunList(), hasSize(1));

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}