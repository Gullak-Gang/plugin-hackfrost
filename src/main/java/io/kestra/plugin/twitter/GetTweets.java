package io.kestra.plugin.twitter;

import io.kestra.service.*;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.slf4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(title = "Fetch Tweets using a hashtag", description = "Fetch posts from Twitter containing a specific hashtag")
@Plugin(examples = {
        @io.kestra.core.models.annotations.Example(title = "Fetch Tweets", code = {
                "hashtag: blackfridaysale",
                "numberOfPosts: 1"
        })
})
public class GetTweets extends Task implements RunnableTask<GetTweets.Output> {
    private static final String TWITTER_ACCESS_TOKEN_KEY = "twitter_access_token";
    private static final String TWITTER_REFRESH_TOKEN_KEY = "twitter_refresh_token";
    private static final String TWITTER_TOKEN_EXPIRES_AT_KEY = "twitter_token_expires_at";
    private static final String CLIENT_ID = "twitter_client_id"; // "rG9n6402A3dbUJKzXTNX4oWHJ";
    //
    private static Logger logger;

    @NonNull
    @Schema(title = "Access Token", description = "Access Token Generated after the OAuth2 process")
    @PluginProperty(dynamic = true)
    private String access_token;

    @NonNull
    @Schema(title = "Refresh Token", description = "Refresh Token Generated after the OAuth2 process")
    @PluginProperty(dynamic = true)
    private String refresh_token;

    @NonNull
    @Schema(title = "Expires At", description = "Expires At Generated after the OAuth2 process")
    @PluginProperty(dynamic = true)
    private String expires_at;

    @NonNull
    @Schema(title = "Client ID", description = "Client ID from twitter developer console")
    @PluginProperty(dynamic = true)
    private String client_id;

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
        logger = runContext.logger();
        // Render dynamic inputs
        String inputAccessToken = runContext.render(access_token);
        String inputRefreshToken = runContext.render(refresh_token);
        String inputExpiresAt = runContext.render(expires_at);
        String inputClientId = runContext.render(client_id);
        String inputNumberOfPosts = runContext.render(numberOfPosts);
        String inputHashtag = runContext.render(hashtag).replaceFirst("^#", "");
        File tempFile = runContext.workingDir().createTempFile().toFile();

        ObjectMapper om = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        KeyValueStoreService kvService = new KeyValueStoreService(runContext);
        // Get current tokens from KV store

        // Check and refresh token if needed
        if (inputAccessToken == null || inputRefreshToken == null || inputExpiresAt == null ||
                System.currentTimeMillis() >= Long.parseLong(inputExpiresAt)) {

            // Refresh token
            TokenRefreshResponse refreshTokenResponse = refreshAccessToken(inputRefreshToken, inputClientId);
            inputAccessToken = refreshTokenResponse.access_token;

            // Save new tokens to KV store
            kvService.addKeyValue(TWITTER_ACCESS_TOKEN_KEY, refreshTokenResponse.access_token);
            kvService.addKeyValue(TWITTER_REFRESH_TOKEN_KEY, refreshTokenResponse.refresh_token);
            kvService.addKeyValue(TWITTER_TOKEN_EXPIRES_AT_KEY, String.valueOf(refreshTokenResponse.expires_at));
        }

        // Fetch tweets
        List<String> tweets = fetchTweets(inputAccessToken, inputHashtag, inputNumberOfPosts);

        try (Writer output = new BufferedWriter(new FileWriter(tempFile))) {

            output.write(om.writeValueAsString(tweets));
            output.flush();
            logger.debug("tempFile: {}", tempFile);

            URI tempFileURI = runContext.storage().putFile(tempFile);

            logger.debug("tempFileURI: {}", tempFileURI);

            return Output.builder().uri(tempFileURI).build();
        }
    }

    private TokenRefreshResponse refreshAccessToken(String refreshToken, String clientID)
            throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Prepare token refresh request
        HttpRequest refreshRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.x.com/2/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "refresh_token=" + refreshToken +
                                "&grant_type=refresh_token" +
                                "&client_id=" + clientID))
                .build();

        // Send refresh request
        HttpResponse<String> refreshResponse = httpClient.send(refreshRequest, HttpResponse.BodyHandlers.ofString());
        logger.debug("token refresh response: {}", refreshResponse.body());
        return objectMapper.readValue(refreshResponse.body(), TokenRefreshResponse.class);
    }

    private List<String> fetchTweets(String accessToken, String hashtag, String maxResults)
            throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Prepare tweets search request
        HttpRequest tweetsRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twitter.com/2/tweets/search/recent?query=%23" + hashtag + "&max_results="
                        + maxResults))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        // Send tweets request
        // HttpResponse<String> tweetsResponse = httpClient.send(tweetsRequest,
        // HttpResponse.BodyHandlers.ofString());
        // logger.debug("tweets response: {}\nRequest URL: {}", tweetsResponse.body(),
        // "https://api.twitter.com/2/tweets/search/recent?query=%23" + hashtag +
        // "&max_results="
        // + maxResults);
        // TweetsData tweetsObject = objectMapper.readValue(tweetsResponse.body(),
        // TweetsData.class);
        String tweetsResponseHardCoded = "{\"data\":[{\"text\":\"RT @SmartBiology3D: \\uD83C\\uDF8950% OFF Black Friday event!!! \\n\\nSave BIG on our online animated Biology courses now through December 2! Don’t miss out…\",\"edit_history_tweet_ids\":[\"1863004872775291239\"],\"id\":\"1863004872775291239\"},{\"text\":\"RT @AmbSzweWarszawa: Mi\\u0142ego pi\\u0105teczku.\\n\\n#BlackFriday https://t.co/tFcEs7fSw8\",\"edit_history_tweet_ids\":[\"1863004866462822606\"],\"id\":\"1863004866462822606\"},{\"text\":\"RT @GretaFanti: Bianca \\u00e8 stata salvata da un'alluvione,stava per annegare e una persona buona l'ha messa al sicuro. Ha 4 mesi futura taglia…\",\"edit_history_tweet_ids\":[\"1863004852843892806\"],\"id\":\"1863004852843892806\"},{\"text\":\"RT @ZEbetFr: \\uD83C\\uDF81 \\uD835\\uDC14\\uD835\\uDC27 \\uD835\\uDC26\\uD835\\uDC1A\\uD835\\uDC22\\uD835\\uDC25\\uD835\\uDC25\\uD835\\uDC28\\uD835\\uDC2D \\uD835\\uDC1A\\u0300 \\uD835\\uDC20\\uD835\\uDC1A\\uD835\\uDC20\\uD835\\uDC27\\uD835\\uDC1E\\uD835\\uDC2B \\uD835\\uDC2C\\uD835\\uDC2E\\uD835\\uDC2B \\uD835\\uDC19\\uD835\\uDC04\\uD835\\uDC1B\\uD835\\uDC1E\\uD835\\uDC2D !\\n\\n\\u00c0 l'occasion du #BlackFriday, on vous offre un maillot du club de votre choix !\\n\\n\\uD83D\\uDD01+❤️+\\uD83D\\uDCAC av…\",\"edit_history_tweet_ids\":[\"1863004851640091032\"],\"id\":\"1863004851640091032\"},{\"text\":\"RT @TantalyGlobal: MASSAGE TIME\\uD83D\\uDE4C\\uD83D\\uDCA6\\nGet one now \\uD83D\\uDC49https://t.co/MhiwJmahE9\\n\\n#Tantaly #massage #blackfriday #FridayVibes https://t.co/wj1dq4r2Kn\",\"edit_history_tweet_ids\":[\"1863004836402462946\"],\"id\":\"1863004836402462946\"},{\"text\":\"RT @RakutenFrance: #CONCOURS \\uD83C\\uDF81\\n\\nPendant le #BlackFriday, tentez de remporter un iPhone 16 noir 128 Go \\uD83D\\uDC49 https://t.co/LQ0Clpq5fr\\n\\nPour parti…\",\"edit_history_tweet_ids\":[\"1863004817347506309\"],\"id\":\"1863004817347506309\"},{\"text\":\"RT @TantalyFantasy: Ride on it @Tantalygloal \\nBlack friday big deal\\uD83D\\uDD25\\uD83D\\uDED2\\uD83D\\uDECD️ https://t.co/YNYfYLRGfX\\n\\n#Tantaly #blackfriday #blackfirdaydeals ht…\",\"edit_history_tweet_ids\":[\"1863004812616548541\"],\"id\":\"1863004812616548541\"},{\"text\":\"RT @DiegoKush3: Ver como estan penetrando a mi mujer de perrita\\uD83C\\uDF51\\uD83D\\uDC70\\u200D♀️\\uD83D\\uDC29 mientras le meten los dedos\\uD83D\\uDE0D es m\\u00e1gico, ella se merece 2 vergotas den…\",\"edit_history_tweet_ids\":[\"1863004809416101939\"],\"id\":\"1863004809416101939\"},{\"text\":\"RT @ClarasJewelry: Sarah Coventry Taste Of Honey Necklace Dangle Clip On Earring Set\\n#rubylane #vintage #necklace #set #bargains #vintageje…\",\"edit_history_tweet_ids\":[\"1863004808635945145\"],\"id\":\"1863004808635945145\"},{\"text\":\"Ayuda que ya casi lo gano\\n#shein\\n#cachorrofeliz\\n#BlackFriday\\n#blackfridayshein\\nhttps://t.co/WtUXwAwrqt\",\"edit_history_tweet_ids\":[\"1863004807411147201\"],\"id\":\"1863004807411147201\"}],\"meta\":{\"newest_id\":\"1863004872775291239\",\"oldest_id\":\"1863004807411147201\",\"result_count\":10,\"next_token\":\"b26v89c19zqg8o3frr3s4ss8olsod2eqdg4ua9h8ig29p\"}}";

        TweetsData tweetsObject = objectMapper.readValue(tweetsResponseHardCoded, TweetsData.class);

        // Extract tweet texts
        List<String> tweets = new ArrayList<>();
        for (Tweet tweet : tweetsObject.data) {
            tweets.add(tweet.text);
        }

        return tweets;
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "URI of a temporary result file containing an array of tweets")
        private final URI uri;

        @Schema(title = "Date of Data")
        private final String currentDate;
    }
}