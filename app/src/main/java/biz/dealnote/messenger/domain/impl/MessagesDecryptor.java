package biz.dealnote.messenger.domain.impl;

import android.util.LongSparseArray;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.crypt.AesKeyPair;
import biz.dealnote.messenger.crypt.CryptHelper;
import biz.dealnote.messenger.crypt.EncryptedMessage;
import biz.dealnote.messenger.db.interfaces.IStorages;
import biz.dealnote.messenger.domain.IMessagesDecryptor;
import biz.dealnote.messenger.model.CryptStatus;
import biz.dealnote.messenger.model.Message;
import biz.dealnote.messenger.util.Pair;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleTransformer;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;

public class MessagesDecryptor implements IMessagesDecryptor {

    private final IStorages store;

    public MessagesDecryptor(IStorages store) {
        this.store = store;
    }

    @Override
    public SingleTransformer<List<Message>, List<Message>> withMessagesDecryption(int accountId) {
        return single -> single
                .flatMap(messages -> {
                    List<Pair<Integer, Long>> sessions = new ArrayList<>(0);
                    List<Pair<Message, EncryptedMessage>> needDecryption = new ArrayList<>(0);

                    for (Message message : messages) {
                        if (message.getCryptStatus() != CryptStatus.ENCRYPTED) {
                            continue;
                        }

                        try {
                            EncryptedMessage em = CryptHelper.parseEncryptedMessage(message.getBody());

                            if (nonNull(em)) {
                                needDecryption.add(Pair.Companion.create(message, em));
                                sessions.add(Pair.Companion.create(em.getKeyLocationPolicy(), em.getSessionId()));
                            } else {
                                message.setCryptStatus(CryptStatus.DECRYPT_FAILED);
                            }
                        } catch (Exception e) {
                            message.setCryptStatus(CryptStatus.DECRYPT_FAILED);
                        }
                    }

                    if (needDecryption.isEmpty()) {
                        return Single.just(messages);
                    }

                    return getKeyPairs(accountId, sessions)
                            .map(keys -> {
                                for (Pair<Message, EncryptedMessage> pair : needDecryption) {
                                    Message message = pair.getFirst();
                                    EncryptedMessage em = pair.getSecond();

                                    try {
                                        AesKeyPair keyPair = keys.get(em.getSessionId());

                                        if (isNull(keyPair)) {
                                            message.setCryptStatus(CryptStatus.DECRYPT_FAILED);
                                            continue;
                                        }

                                        String key = message.isOut() ? keyPair.getMyAesKey() : keyPair.getHisAesKey();
                                        String decryptedBody = CryptHelper.decryptWithAes(em.getOriginalBody(), key);

                                        message.setDecryptedBody(decryptedBody);
                                        message.setCryptStatus(CryptStatus.DECRYPTED);
                                    } catch (Exception e) {
                                        message.setCryptStatus(CryptStatus.DECRYPT_FAILED);
                                    }
                                }

                                return messages;
                            });
                });
    }

    private Single<LongSparseArray<AesKeyPair>> getKeyPairs(int accountId, List<Pair<Integer, Long>> tokens) {
        return Single.create(emitter -> {
            LongSparseArray<AesKeyPair> keys = new LongSparseArray<>(tokens.size());

            for (Pair<Integer, Long> token : tokens) {
                if (emitter.isDisposed()) {
                    break;
                }

                long sessionId = token.getSecond();
                int keyPolicy = token.getFirst();

                AesKeyPair keyPair = store.keys(keyPolicy).findKeyPairFor(accountId, sessionId).blockingGet();

                if (nonNull(keyPair)) {
                    keys.append(sessionId, keyPair);
                }
            }

            emitter.onSuccess(keys);
        });
    }
}