package biz.dealnote.messenger.api.services;

import biz.dealnote.messenger.api.model.Items;
import biz.dealnote.messenger.api.model.VKApiUser;
import biz.dealnote.messenger.api.model.VkApiFriendList;
import biz.dealnote.messenger.api.model.response.BaseResponse;
import biz.dealnote.messenger.api.model.response.DeleteFriendResponse;
import biz.dealnote.messenger.api.model.response.MutualFriendsResponse;
import biz.dealnote.messenger.api.model.response.OnlineFriendsResponse;
import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface IFriendsService {

    @FormUrlEncoded
    @POST("execute")
    Single<BaseResponse<OnlineFriendsResponse>> getOnline(@Field("code") String code);

    /*@FormUrlEncoded
    @POST("execute")
    Single<BaseResponse<FriendsWithCountersResponse>> getWithMyCounters(@Field("code") String code);*/

    @FormUrlEncoded
    @POST("friends.get")
    Single<BaseResponse<Items<VKApiUser>>> get(@Field("user_id") Integer userId,
                                               @Field("order") String order,
                                               @Field("list_id") Integer listId,
                                               @Field("count") Integer count,
                                               @Field("offset") Integer offset,
                                               @Field("fields") String fields,
                                               @Field("name_case") String nameCase);

    @FormUrlEncoded
    @POST("friends.getRecommendations")
    Single<BaseResponse<Items<VKApiUser>>> getRecommendations(@Field("count") Integer count,
                                                              @Field("fields") String fields,
                                                              @Field("name_case") String nameCase);

    //https://vk.com/dev/friends.getLists
    @FormUrlEncoded
    @POST("friends.getLists")
    Single<BaseResponse<Items<VkApiFriendList>>> getLists(@Field("user_id") Integer userId,
                                                          @Field("return_system") Integer returnSystem);

    //https://vk.com/dev/friends.delete
    @FormUrlEncoded
    @POST("friends.delete")
    Single<BaseResponse<DeleteFriendResponse>> delete(@Field("user_id") int userId);

    //https://vk.com/dev/friends.add
    @FormUrlEncoded
    @POST("friends.add")
    Single<BaseResponse<Integer>> add(@Field("user_id") int userId,
                                      @Field("text") String text,
                                      @Field("follow") Integer follow);

    //https://vk.com/dev/friends.search
    @FormUrlEncoded
    @POST("friends.search")
    Single<BaseResponse<Items<VKApiUser>>> search(@Field("user_id") int userId,
                                                  @Field("q") String query,
                                                  @Field("fields") String fields,
                                                  @Field("name_case") String nameCase,
                                                  @Field("offset") Integer offset,
                                                  @Field("count") Integer count);

    @FormUrlEncoded
    @POST("execute")
    Single<BaseResponse<MutualFriendsResponse>> getMutual(@Field("code") String code);

}
