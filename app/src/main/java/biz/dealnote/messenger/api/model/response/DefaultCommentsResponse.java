package biz.dealnote.messenger.api.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import biz.dealnote.messenger.api.model.VKApiComment;
import biz.dealnote.messenger.api.model.VKApiCommunity;
import biz.dealnote.messenger.api.model.VKApiUser;

public class DefaultCommentsResponse {

    @SerializedName("count")
    public int count;

    @SerializedName("items")
    public List<VKApiComment> items;

    @SerializedName("groups")
    public List<VKApiCommunity> groups;

    @SerializedName("profiles")
    public List<VKApiUser> profiles;

}
