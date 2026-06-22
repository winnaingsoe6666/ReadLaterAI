package com.knowvault.import_.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RawContent {

    public static final String TYPE_POST = "post";
    public static final String TYPE_SAVED_ITEM = "saved_item";
    public static final String TYPE_MESSENGER_MESSAGE = "messenger_message";
    public static final String TYPE_GROUP_POST = "group_post";
    public static final String TYPE_GROUP_COMMENTED_POST = "group_commented_post";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_LIKED_PAGE = "liked_page";
    public static final String TYPE_AD_PREFERENCE = "ad_preference";

    private String title;
    private String contentText;
    private String url;
    private String timestamp;
    private String author;
    private String sourceType;
    @Builder.Default
    private Map<String, String> metadata = Map.of();
}
