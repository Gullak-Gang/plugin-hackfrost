package io.kestra.plugin.instagram;

import java.util.*;
import lombok.*;

@Data
public class PostsData {
    public String inputUrl;
    public String id;
    public String type;
    public String shortCode;
    public String caption;
    public ArrayList<String> hashtags;
    public String url;
    public int commentsCount;
    public String firstComment;
    public ArrayList<Object> latestComments;
    public int dimensionsHeight;
    public int dimensionsWidth;
    public String displayUrl;
    public ArrayList<Object> images;
    public ArrayList<Object> likers;
    public Date timestamp;
    public ArrayList<Object> childPosts;
    public String locationName;
    public long locationId;
    public String ownerFullName;
    public String ownerUsername;
    public String ownerId;
    public String productType;
    public boolean isSponsored;
    public MusicInfo musicInfo;
    public ArrayList<String> mentions;
}

@Data
class AudioMutingInfo {
    public boolean allow_audio_editing;
    public boolean mute_audio;
    public String mute_reason_str;
    public boolean show_muted_audio_toast;
}

@Data
class IgArtist {
    public String full_name;
    public String id;
    public boolean is_private;
    public boolean is_verified;
    public String profile_pic_id;
    public String profile_pic_url;
    public String username;
}

@Data
class MusicAssetInfo {
    public boolean allows_saving;
    public String artist_id;
    public String audio_asset_id;
    public String audio_id;
    public String cover_artwork_thumbnail_uri;
    public String cover_artwork_uri;
    public Object dark_message;
    public String display_artist;
    public int duration_in_ms;
    public String fast_start_progressive_download_url;
    public boolean has_lyrics;
    public ArrayList<Integer> highlight_start_times_in_ms;
    public String id;
    public String ig_username;
    public boolean is_eligible_for_audio_effects;
    public boolean is_eligible_for_vinyl_sticker;
    public boolean is_explicit;
    public Object lyrics;
    public String progressive_download_url;
    public Object reactive_audio_download_url;
    public Object sanitized_title;
    public String subtitle;
    public String title;
    public Object web_30s_preview_download_url;
}

@Data
class MusicConsumptionInfo {
    public boolean allow_media_creation_with_music;
    public int audio_asset_start_time_in_ms;
    public ArrayList<Object> audio_filter_infos;
    public AudioMutingInfo audio_muting_info;
    public Object contains_lyrics;
    public Object derived_content_id;
    public Object display_labels;
    public Object formatted_clips_media_count;
    public IgArtist ig_artist;
    public boolean is_bookmarked;
    public boolean is_trending_in_clips;
    public int overlap_duration_in_ms;
    public String placeholder_profile_pic_url;
    public Object previous_trend_rank;
    public boolean should_allow_music_editing;
    public boolean should_mute_audio;
    public String should_mute_audio_reason;
    public Object should_mute_audio_reason_type;
    public Object trend_rank;
}

@Data
class MusicInfo {
    public String audio_canonical_id;
    public String audio_type;
    public MusicInfo music_info;
    public Object original_sound_info;
    public ArrayList<Object> pinned_media_ids;
}

@Data
class MusicInfo2 {
    public MusicAssetInfo music_asset_info;
    public MusicConsumptionInfo music_consumption_info;
}