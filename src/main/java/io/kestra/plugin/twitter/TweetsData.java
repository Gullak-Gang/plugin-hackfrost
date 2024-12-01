package io.kestra.plugin.twitter;

import java.util.*;
import lombok.*;

@Data
public class TweetsData {
    public ArrayList<Tweet> data;
    public Meta meta;
}

@Data
class Token {
    public String status;
    public String message;
    public String token;
}

@Data
class Tweet {
    public String text;
    public String id;
    public ArrayList<String> edit_history_tweet_ids;
}

@Data
class Meta {
    public String newest_id;
    public String oldest_id;
    public int result_count;
    public String next_token;
}

@Data
class TokenRefreshResponse {
    public String refresh_token;
    public String access_token;
    public long expires_at;
}