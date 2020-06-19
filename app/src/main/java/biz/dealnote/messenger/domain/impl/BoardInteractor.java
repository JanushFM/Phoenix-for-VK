package biz.dealnote.messenger.domain.impl;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.api.interfaces.INetworker;
import biz.dealnote.messenger.api.model.VKApiTopic;
import biz.dealnote.messenger.db.interfaces.IStorages;
import biz.dealnote.messenger.db.model.entity.OwnerEntities;
import biz.dealnote.messenger.db.model.entity.TopicEntity;
import biz.dealnote.messenger.domain.IBoardInteractor;
import biz.dealnote.messenger.domain.IOwnersRepository;
import biz.dealnote.messenger.domain.mappers.Dto2Entity;
import biz.dealnote.messenger.domain.mappers.Dto2Model;
import biz.dealnote.messenger.domain.mappers.Entity2Model;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.Topic;
import biz.dealnote.messenger.model.criteria.TopicsCriteria;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.VKOwnIds;
import io.reactivex.Single;


public class BoardInteractor implements IBoardInteractor {

    private final INetworker networker;
    private final IStorages stores;
    private final IOwnersRepository ownersRepository;

    public BoardInteractor(INetworker networker, IStorages stores, IOwnersRepository ownersRepository) {
        this.networker = networker;
        this.stores = stores;
        this.ownersRepository = ownersRepository;
    }

    @Override
    public Single<List<Topic>> getCachedTopics(int accountId, int ownerId) {
        TopicsCriteria criteria = new TopicsCriteria(accountId, ownerId);
        return stores.topics()
                .getByCriteria(criteria)
                .flatMap(dbos -> {
                    VKOwnIds ids = new VKOwnIds();
                    for (TopicEntity dbo : dbos) {
                        ids.append(dbo.getCreatorId());
                        ids.append(dbo.getUpdatedBy());
                    }

                    return ownersRepository.findBaseOwnersDataAsBundle(accountId, ids.getAll(), IOwnersRepository.MODE_ANY)
                            .map(owners -> {
                                List<Topic> topics = new ArrayList<>(dbos.size());
                                for (TopicEntity dbo : dbos) {
                                    topics.add(Entity2Model.buildTopicFromDbo(dbo, owners));
                                }
                                return topics;
                            });
                });
    }

    //public static final int ORDER_DESCENDING_UPDATE_TIME = 1;
    //public static final int ORDER_DESCENDING_CREATE_TIME = 2;
    //public static final int ORDER_ASCENDING_UPDATE_TIME = -1;
    //public static final int ORDER_ASCENDING_CREATE_TIME = -2;

    @Override
    public Single<List<Topic>> getActualTopics(int accountId, int ownerId, int count, int offset) {
        return networker.vkDefault(accountId)
                .board()
                .getTopics(Math.abs(ownerId), null, null, offset, count, true, null, null, Constants.MAIN_OWNER_FIELDS)
                .flatMap(response -> {
                    List<VKApiTopic> dtos = Utils.listEmptyIfNull(response.items);
                    List<TopicEntity> dbos = new ArrayList<>(dtos.size());

                    for (VKApiTopic dto : dtos) {
                        dbos.add(Dto2Entity.buildTopicDbo(dto));
                    }

                    final OwnerEntities ownerEntities = Dto2Entity.mapOwners(response.profiles, response.groups);

                    VKOwnIds ownerIds = new VKOwnIds();
                    for (TopicEntity dbo : dbos) {
                        ownerIds.append(dbo.getCreatorId());
                        ownerIds.append(dbo.getUpdatedBy());
                    }

                    final List<Owner> owners = Dto2Model.transformOwners(response.profiles, response.groups);

                    return stores.topics()
                            .store(accountId, ownerId, dbos, ownerEntities, response.canAddTopics == 1, response.defaultOrder, offset == 0)
                            .andThen(ownersRepository.findBaseOwnersDataAsBundle(accountId, ownerIds.getAll(), IOwnersRepository.MODE_ANY, owners)
                                    .map(ownersBundle -> {
                                        List<Topic> topics = new ArrayList<>(dbos.size());
                                        for (TopicEntity dbo : dbos) {
                                            topics.add(Entity2Model.buildTopicFromDbo(dbo, ownersBundle));
                                        }
                                        return topics;
                                    }));
                });
    }
}