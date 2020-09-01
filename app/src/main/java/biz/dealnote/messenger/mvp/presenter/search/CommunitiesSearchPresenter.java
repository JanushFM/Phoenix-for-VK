package biz.dealnote.messenger.mvp.presenter.search;

import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.List;

import biz.dealnote.messenger.domain.ICommunitiesInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.fragment.search.criteria.GroupSearchCriteria;
import biz.dealnote.messenger.fragment.search.nextfrom.IntNextFrom;
import biz.dealnote.messenger.fragment.search.options.SpinnerOption;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.mvp.view.search.ICommunitiesSearchView;
import biz.dealnote.messenger.util.Pair;
import io.reactivex.rxjava3.core.Single;

import static biz.dealnote.messenger.util.Utils.nonEmpty;

public class CommunitiesSearchPresenter extends AbsSearchPresenter<ICommunitiesSearchView,
        GroupSearchCriteria, Community, IntNextFrom> {

    private final ICommunitiesInteractor communitiesInteractor;

    public CommunitiesSearchPresenter(int accountId, @Nullable GroupSearchCriteria criteria, @Nullable Bundle savedInstanceState) {
        super(accountId, criteria, savedInstanceState);
        communitiesInteractor = InteractorFactory.createCommunitiesInteractor();
    }

    private static String extractTypeFromCriteria(GroupSearchCriteria criteria) {
        SpinnerOption option = criteria.findOptionByKey(GroupSearchCriteria.KEY_TYPE);
        if (option != null && option.value != null) {
            switch (option.value.id) {
                case GroupSearchCriteria.TYPE_PAGE:
                    return "page";
                case GroupSearchCriteria.TYPE_GROUP:
                    return "group";
                case GroupSearchCriteria.TYPE_EVENT:
                    return "event";
            }
        }

        return null;
    }

    @Override
    IntNextFrom getInitialNextFrom() {
        return new IntNextFrom(0);
    }

    @Override
    boolean isAtLast(IntNextFrom startFrom) {
        return startFrom.getOffset() == 0;
    }

    @Override
    Single<Pair<List<Community>, IntNextFrom>> doSearch(int accountId, GroupSearchCriteria criteria, IntNextFrom startFrom) {
        String type = extractTypeFromCriteria(criteria);

        Integer countryId = criteria.extractDatabaseEntryValueId(GroupSearchCriteria.KEY_COUNTRY);
        Integer cityId = criteria.extractDatabaseEntryValueId(GroupSearchCriteria.KEY_CITY);
        Boolean future = criteria.extractBoleanValueFromOption(GroupSearchCriteria.KEY_FUTURE_ONLY);

        SpinnerOption sortOption = criteria.findOptionByKey(GroupSearchCriteria.KEY_SORT);
        Integer sort = (sortOption == null || sortOption.value == null) ? null : sortOption.value.id;

        int offset = startFrom.getOffset();
        IntNextFrom nextFrom = new IntNextFrom(offset + 50);

        return communitiesInteractor.search(accountId, criteria.getQuery(), type, countryId, cityId, future, sort, 50, offset)
                .map(communities -> Pair.Companion.create(communities, nextFrom));
    }

    @Override
    GroupSearchCriteria instantiateEmptyCriteria() {
        return new GroupSearchCriteria("");
    }

    @Override
    boolean canSearch(GroupSearchCriteria criteria) {
        return nonEmpty(criteria.getQuery());
    }

    public void fireCommunityClick(Community community) {
        getView().openCommunityWall(getAccountId(), community);
    }
}