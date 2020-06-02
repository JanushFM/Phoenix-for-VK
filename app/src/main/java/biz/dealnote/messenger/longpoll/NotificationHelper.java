package biz.dealnote.messenger.longpoll;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.MainActivity;
import biz.dealnote.messenger.activity.QuickAnswerActivity;
import biz.dealnote.messenger.api.model.VKApiMessage;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.link.internal.OwnerLinkSpanFactory;
import biz.dealnote.messenger.model.Message;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.Peer;
import biz.dealnote.messenger.model.PhotoSize;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.push.ChatEntryFetcher;
import biz.dealnote.messenger.push.NotificationScheduler;
import biz.dealnote.messenger.push.NotificationUtils;
import biz.dealnote.messenger.service.QuickReplyService;
import biz.dealnote.messenger.settings.ISettings;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;

import static biz.dealnote.messenger.util.Utils.hasFlag;
import static biz.dealnote.messenger.util.Utils.isEmpty;

public class NotificationHelper {

    public static final int NOTIFICATION_MESSAGE = 62;
    public static final int NOTIFICATION_WALL_POST_ID = 63;
    public static final int NOTIFICATION_REPLY_ID = 64;
    public static final int NOTIFICATION_COMMENT_ID = 65;
    public static final int NOTIFICATION_WALL_PUBLISH_ID = 66;
    public static final int NOTIFICATION_FRIEND_ID = 67;
    public static final int NOTIFICATION_FRIEND_ACCEPTED_ID = 68;
    public static final int NOTIFICATION_GROUP_INVITE_ID = 69;
    public static final int NOTIFICATION_NEW_POSTS_ID = 70;
    public static final int NOTIFICATION_LIKE = 71;
    public static final int NOTIFICATION_BIRTHDAY = 72;
    public static final int NOTIFICATION_UPLOAD = 73;
    public static final int NOTIFICATION_DOWNLOADING = 74;
    public static final int NOTIFICATION_DOWNLOAD = 75;

    /**
     * Отображение уведомления в statusbar о новом сообщении.
     * Этот метод сначала в отдельном потоке получает всю необходимую информацию для отображения
     *
     * @param context контекст
     */

    @SuppressLint("CheckResult")
    public static void notifNewMessage(final Context context, final int accountId, final Message message) {
        ChatEntryFetcher.getRx(context, accountId, accountId)
                .subscribeOn(NotificationScheduler.INSTANCE)
                .subscribe(account -> ChatEntryFetcher.getRx(context, accountId, message.getPeerId())
                        .subscribeOn(NotificationScheduler.INSTANCE)
                        .subscribe(info -> {
                            if (Settings.get().main().isLoad_history_notif()) {
                                Repository.INSTANCE.getMessages().getPeerMessages(accountId, message.getPeerId(), 10, 1, null, false, false)
                                        .subscribeOn(NotificationScheduler.INSTANCE)
                                        .subscribe(history -> {
                                            Peer account_peer = new Peer(accountId).setTitle(account.title).setAvaUrl(account.img);
                                            Peer peer = new Peer(message.getPeerId()).setTitle(info.title).setAvaUrl(info.img);
                                            showNotification(context, accountId, peer, message, info.icon, history, account_peer, account.icon);
                                        }, t -> {
                                            Peer account_peer = new Peer(accountId).setTitle(account.title).setAvaUrl(account.img);
                                            Peer peer = new Peer(message.getPeerId()).setTitle(info.title).setAvaUrl(info.img);
                                            showNotification(context, accountId, peer, message, info.icon, null, account_peer, account.icon);
                                        });
                            } else {
                                Peer account_peer = new Peer(accountId).setTitle(account.title).setAvaUrl(account.img);
                                Peer peer = new Peer(message.getPeerId()).setTitle(info.title).setAvaUrl(info.img);
                                showNotification(context, accountId, peer, message, info.icon, null, account_peer, account.icon);
                            }
                        }, RxUtils.ignore()), RxUtils.ignore());
    }

    private static String getSenderName(Owner own, Context ctx) {
        if (own == null || isEmpty(own.getFullName()))
            return ctx.getString(R.string.error);
        return own.getFullName();
    }

    private static CharSequence getMessageContent(boolean hideBody, Message message, Context context) {
        String messageText = Utils.isEmpty(message.getDecryptedBody()) ? message.getBody() : message.getDecryptedBody();
        if (messageText == null)
            messageText = "";

        if (message.getForwardMessagesCount() > 0) {
            messageText += " " + context.getString(R.string.notif_forward, message.getForwardMessagesCount());
        }
        if (message.getAttachments() != null && message.getAttachments().size() > 0) {
            if (!isEmpty(message.getAttachments().getStickers()))
                messageText += " " + context.getString(R.string.notif_sticker);
            else
                messageText += " " + context.getString(R.string.notif_attach, message.getAttachments().size(), context.getString(Utils.declOfNum(message.getAttachments().size(), new int[]{R.string.attachment_notif, R.string.attacment_sec_notif, R.string.attachments_notif})));
        }

        String text = hideBody ? context.getString(R.string.message_text_is_not_available) : messageText;
        return OwnerLinkSpanFactory.withSpans(text, true, false, null);
    }

    @SuppressLint("CheckResult")
    public static void showNotification(Context context, int accountId, Peer peer, Message message, Bitmap avatar, List<Message> History, Peer ich, Bitmap acc_avatar) {
        boolean hideBody = Settings.get()
                .security()
                .needHideMessagesBodyForNotif();

        CharSequence text = getMessageContent(hideBody, message, context);

        final NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Objects.isNull(nManager)) {
            return;
        }

        if (Utils.hasOreo()) {
            nManager.createNotificationChannel(AppNotificationChannels.getChatMessageChannel(context));
            nManager.createNotificationChannel(AppNotificationChannels.getGroupChatMessageChannel(context));
        }

        String channelId = Peer.isGroupChat(message.getPeerId()) ? AppNotificationChannels.GROUP_CHAT_MESSAGE_CHANNEL_ID :
                AppNotificationChannels.CHAT_MESSAGE_CHANNEL_ID;


        NotificationCompat.MessagingStyle msgs = new NotificationCompat.MessagingStyle(new Person.Builder()
                .setName(ich.getTitle()).setIcon(IconCompat.createWithBitmap(acc_avatar))
                .setKey(String.valueOf(accountId)).build());


        NotificationCompat.MessagingStyle.Message inbox = new NotificationCompat.MessagingStyle.Message(text,
                message.getDate() * 1000,
                new Person.Builder()
                        .setName(getSenderName(message.getSender(), context)).setIcon(IconCompat.createWithBitmap(message.getSenderId() == accountId ? acc_avatar : avatar))
                        .setKey(String.valueOf(message.getSenderId())).build());


        if (History != null) {
            Collections.reverse(History);
            for (Message i : History) {
                NotificationCompat.MessagingStyle.Message h_inbox = new NotificationCompat.MessagingStyle.Message(getMessageContent(hideBody, i, context),
                        i.getDate() * 1000,
                        new Person.Builder()
                                .setName(getSenderName(i.getSender(), context)).setIcon(IconCompat.createWithBitmap(i.getSenderId() == accountId ? acc_avatar : avatar))
                                .setKey(String.valueOf(i.getSenderId())).build());
                msgs.addMessage(h_inbox);
            }
        }

        msgs.addMessage(inbox);

        if (message.getPeerId() > VKApiMessage.CHAT_PEER) {
            msgs.setConversationTitle(peer.getTitle());
            msgs.setGroupConversation(Build.VERSION.SDK_INT >= 28);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.phoenix_round)
                .setContentText(text)
                .setStyle(msgs)
                .setColor(Utils.getThemeColor())
                .setWhen(message.getDate() * 1000)
                .setShowWhen(true)
                .setSortKey("" + (Long.MAX_VALUE - message.getDate() * 1000))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true);

        int notificationMask = Settings.get()
                .notifications()
                .getNotifPref(accountId, message.getPeerId());

        if (hasFlag(notificationMask, ISettings.INotificationSettings.FLAG_HIGH_PRIORITY)) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        //Our quickreply
        Intent intentQuick = QuickAnswerActivity.forStart(context, accountId, message, text.toString(), peer.getAvaUrl(), peer.getTitle());
        PendingIntent quickPendingIntent = PendingIntent.getActivity(context, message.getId(), intentQuick, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action actionCustomReply = new NotificationCompat.Action(R.drawable.reply, context.getString(R.string.quick_answer_title), quickPendingIntent);

        //System reply. Works only on Wear (20 Api) and N+
        RemoteInput remoteInput = new RemoteInput.Builder(Extra.BODY)
                .setLabel(context.getResources().getString(R.string.reply))
                .build();

        Intent ReadIntent = QuickReplyService.intentForReadMessage(context, accountId, message.getPeerId(), message.getId());

        Intent directIntent = QuickReplyService.intentForAddMessage(context, accountId, message.getPeerId(), message);
        PendingIntent ReadPendingIntent = PendingIntent.getService(context, message.getId(), ReadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent directPendingIntent = PendingIntent.getService(context, message.getId(), directIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action actionDirectReply = new NotificationCompat.Action.Builder
                (/*may be missing in some cases*/ R.drawable.reply,
                        context.getResources().getString(R.string.reply), directPendingIntent)
                .addRemoteInput(remoteInput)
                .build();

        NotificationCompat.Action actionRead = new NotificationCompat.Action.Builder
                (/*may be missing in some cases*/ R.drawable.view,
                        context.getResources().getString(R.string.read), ReadPendingIntent)
                .build();

        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(MainActivity.ACTION_OPEN_PLACE);

        Place chatPlace = PlaceFactory.getChatPlace(accountId, accountId, peer, 0);
        intent.putExtra(Extra.PLACE, chatPlace);

        PendingIntent contentIntent = PendingIntent.getActivity(context, message.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(contentIntent);

        if (!Utils.hasNougat()) {
            builder.addAction(actionCustomReply);
        } else {
            builder.addAction(actionDirectReply);
        }

        builder.addAction(actionRead);

        //Для часов игнорирует все остальные action, тем самым убирает QuickReply

        NotificationCompat.WearableExtender War = new NotificationCompat.WearableExtender();
        War.addAction(actionDirectReply);
        War.addAction(actionRead);
        War.setStartScrollBottom(true);

        builder.extend(War);

        if (hasFlag(notificationMask, ISettings.INotificationSettings.FLAG_LED)) {
            builder.setLights(0xFF0000FF, 100, 1000);
        }

        if (hasFlag(notificationMask, ISettings.INotificationSettings.FLAG_VIBRO)) {
            builder.setVibrate(Settings.get()
                    .notifications()
                    .getVibrationLength());
        }

        if (hasFlag(notificationMask, ISettings.INotificationSettings.FLAG_SOUND)) {
            builder.setSound(findNotificationSound());
        }

        nManager.notify(createPeerTagFor(accountId, message.getPeerId()), NOTIFICATION_MESSAGE, builder.build());

        if (!hideBody && message.isHasAttachments() && message.getAttachments() != null && !isEmpty(message.getAttachments().getPhotos())) {
            String url = message.getAttachments().getPhotos().get(0).getUrlForSize(PhotoSize.X, false);

            NotificationUtils.loadImageRx(url)
                    .subscribeOn(NotificationScheduler.INSTANCE)
                    .subscribe(bitmap -> {
                        if (bitmap != null) {
                            builder.setStyle(new NotificationCompat.BigPictureStyle()
                                    .setBigContentTitle(text)
                                    .setSummaryText(context.getString(R.string.notif_photos, message.getAttachments().getPhotos().size()))
                                    .bigPicture(bitmap));
                            nManager.notify(createPeerTagFor(accountId, message.getPeerId()), NOTIFICATION_MESSAGE, builder.build());
                        }
                    }, t -> {
                    });
        } else if (!hideBody && message.isHasAttachments() && message.getAttachments() != null && !isEmpty(message.getAttachments().getStickers())) {
            String url = message.getAttachments().getStickers().get(0).getImage(256, true).getUrl();

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.custom_notification);

            NotificationUtils.loadImageRx(url)
                    .subscribeOn(NotificationScheduler.INSTANCE)
                    .subscribe(bitmap -> {
                        if (bitmap != null) {
                            remoteViews.setBitmap(R.id.image, "setImageBitmap", bitmap);
                            builder.setCustomBigContentView(remoteViews);
                            nManager.notify(createPeerTagFor(accountId, message.getPeerId()), NOTIFICATION_MESSAGE, builder.build());
                        }
                    }, t -> {
                    });
        }

        if (Settings.get().notifications().isQuickReplyImmediately()) {
            Intent startQuickReply = QuickAnswerActivity.forStart(context, accountId, message, text.toString(), peer.getAvaUrl(), peer.getTitle());
            startQuickReply.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startQuickReply.putExtra(QuickAnswerActivity.EXTRA_FOCUS_TO_FIELD, false);
            startQuickReply.putExtra(QuickAnswerActivity.EXTRA_LIVE_DELAY, true);
            context.startActivity(startQuickReply);
        }
    }

    public static void showSimpleNotification(Context context, String body, String Title, String Type) {
        boolean hideBody = Settings.get()
                .security()
                .needHideMessagesBodyForNotif();

        String text = hideBody ? context.getString(R.string.message_text_is_not_available) : body;

        final NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Objects.isNull(nManager)) {
            return;
        }

        if (Utils.hasOreo()) {
            nManager.createNotificationChannel(AppNotificationChannels.getChatMessageChannel(context));
            nManager.createNotificationChannel(AppNotificationChannels.getGroupChatMessageChannel(context));
        }

        if (Type != null)
            Title += ", Type: " + Type;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AppNotificationChannels.CHAT_MESSAGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.phoenix_round)
                .setContentText(text)
                .setContentTitle(Title)
                .setAutoCancel(true);

        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = builder.build();

        nManager.notify("simple " + Settings.get().accounts().getCurrent(), NOTIFICATION_MESSAGE, notification);
    }

    private static String createPeerTagFor(int aid, int peerId) {
        return aid + "_" + peerId;
    }

    public static Uri findNotificationSound() {
        try {
            return Uri.parse(Settings.get()
                    .notifications()
                    .getNotificationRingtone());
        } catch (Exception ignored) {
            return Uri.parse(Settings.get()
                    .notifications()
                    .getDefNotificationRingtone());
        }
    }

    private static NotificationManager getService(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static void tryCancelNotificationForPeer(Context context, int accountId, int peerId) {
        //int mask = Settings.get()
        //        .notifications()
        //        .getNotifPref(accountId, peerId);

        //if (hasFlag(mask, ISettings.INotificationSettings.FLAG_SHOW_NOTIF)) {
        getService(context).cancel(NotificationHelper.createPeerTagFor(accountId, peerId),
                NotificationHelper.NOTIFICATION_MESSAGE);
        //}
    }
}