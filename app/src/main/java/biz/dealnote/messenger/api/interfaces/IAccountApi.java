package biz.dealnote.messenger.api.interfaces;

import androidx.annotation.CheckResult;

import biz.dealnote.messenger.api.model.CountersDto;
import biz.dealnote.messenger.api.model.response.AccountsBannedResponce;
import io.reactivex.Single;


public interface IAccountApi {

    @CheckResult
    Single<Integer> banUser(int userId);

    @CheckResult
    Single<Integer> unbanUser(int userId);

    Single<AccountsBannedResponce> getBanned(Integer count, Integer offset, String fields);

    @CheckResult
    Single<Boolean> unregisterDevice(String deviceId);

    @CheckResult
    Single<Boolean> registerDevice(String token, Integer pushes_granted, String app_version, String push_provider, String companion_apps,
                                   Integer type, String deviceModel, String deviceId, String systemVersion, String settings);

    @CheckResult
    Single<Boolean> setOffline();

    @CheckResult
    Single<CountersDto> getCounters(String filter);
}