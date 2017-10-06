package biz.dealnote.messenger.service;

import android.os.Bundle;

import com.foxykeep.datadroid.exception.CustomRequestException;
import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.service.RequestService;

import biz.dealnote.messenger.exception.ServiceException;
import biz.dealnote.messenger.service.factory.AccountRequestFactory;
import biz.dealnote.messenger.service.factory.AudioRequestFactory;
import biz.dealnote.messenger.service.factory.FaveRequestFactory;
import biz.dealnote.messenger.service.factory.FeedRequestFactory;
import biz.dealnote.messenger.service.factory.MessagesRequestFactory;
import biz.dealnote.messenger.service.factory.PhotoRequestFactory;
import biz.dealnote.messenger.service.factory.UtilsRequestFactory;
import biz.dealnote.messenger.service.operations.account.PushResolveOperation;
import biz.dealnote.messenger.service.operations.audio.AddAudioOperation;
import biz.dealnote.messenger.service.operations.audio.AlbumCoverFindOperation;
import biz.dealnote.messenger.service.operations.audio.DeleteAudioOperation;
import biz.dealnote.messenger.service.operations.audio.RestoreAudioOperation;
import biz.dealnote.messenger.service.operations.audio.SetBroadcastAudioOperation;
import biz.dealnote.messenger.service.operations.docs.DocsDeleteOperation;
import biz.dealnote.messenger.service.operations.fave.FaveGetLinksOperation;
import biz.dealnote.messenger.service.operations.fave.FaveRemoveLinkOperation;
import biz.dealnote.messenger.service.operations.fave.FaveRemoveUserOperation;
import biz.dealnote.messenger.service.operations.feed.FeedGetListOperation;
import biz.dealnote.messenger.service.operations.message.DeleteDialogOperation;
import biz.dealnote.messenger.service.operations.message.DeleteMessageOperation;
import biz.dealnote.messenger.service.operations.message.EditChatOperation;
import biz.dealnote.messenger.service.operations.message.MessagesRestoreOperation;
import biz.dealnote.messenger.service.operations.message.ReadMessageOperation;
import biz.dealnote.messenger.service.operations.photo.CopyPhotoOperation;
import biz.dealnote.messenger.service.operations.photo.CreatePhotoAlbumOperation;
import biz.dealnote.messenger.service.operations.photo.DeletePhotoAlbumOperation;
import biz.dealnote.messenger.service.operations.photo.DeletePhotoOpeation;
import biz.dealnote.messenger.service.operations.photo.EditPhotoAlbumOperation;
import biz.dealnote.messenger.service.operations.photo.GetPhotoByIdOperation;
import biz.dealnote.messenger.service.operations.photo.RestorePhotoOpeation;
import biz.dealnote.messenger.service.operations.utils.ResolveScreenNameOperation;
import biz.dealnote.messenger.util.Objects;

public class RestService extends RequestService {

    @Override
    public Operation getOperationForType(int requestType) {
        switch (requestType) {

            case RequestFactory.REQUEST_READ_MESSAGE:
                return new ReadMessageOperation();

            case RequestFactory.REQUEST_DELETE_MESSAGES:
                return new DeleteMessageOperation();

            case RequestFactory.REQUEST_PHOTOS_BY_ID:
                return new GetPhotoByIdOperation();

            case FeedRequestFactory.REQUEST_GET_LISTS:
                return new FeedGetListOperation();

            case RequestFactory.REQUEST_MESSAGES_RESTORE:
                return new MessagesRestoreOperation();
            case RequestFactory.REQUEST_DOCS_DELETE:
                return new DocsDeleteOperation();

            case MessagesRequestFactory.REQUEST_DELETE_DIALOG:
                return new DeleteDialogOperation();

            case MessagesRequestFactory.REQUEST_EDIT_CHAT:
                return new EditChatOperation();

            case FaveRequestFactory.REQUEST_GET_LINKS:
                return new FaveGetLinksOperation();

            case FaveRequestFactory.REQUEST_REMOVE_USER:
                return new FaveRemoveUserOperation();

            case FaveRequestFactory.REQUEST_REMOVE_LINK:
                return new FaveRemoveLinkOperation();

            case AudioRequestFactory.REQUEST_ADD:
                return new AddAudioOperation();
            case AudioRequestFactory.REQUEST_FIND_COVER:
                return new AlbumCoverFindOperation();
            case AudioRequestFactory.REQUEST_DELETE:
                return new DeleteAudioOperation();
            case AudioRequestFactory.REQUEST_BROADCAST:
                return new SetBroadcastAudioOperation();
            case AudioRequestFactory.REQUEST_RESTORE:
                return new RestoreAudioOperation();

            case UtilsRequestFactory.REQUEST_SCREEN_NAME:
                return new ResolveScreenNameOperation();

            case PhotoRequestFactory.REQUEST_COPY:
                return new CopyPhotoOperation();
            case PhotoRequestFactory.REQUEST_DELETE:
                return new DeletePhotoOpeation();
            case PhotoRequestFactory.REQUEST_RESTORE:
                return new RestorePhotoOpeation();

            case AccountRequestFactory.REQUEST_RESOLVE_PUSH_REGISTRATION:
                return new PushResolveOperation();
            case PhotoRequestFactory.REQUEST_CREATE_ALBUM:
                return new CreatePhotoAlbumOperation();

            case PhotoRequestFactory.REQUEST_DELETE_ALBUM:
                return new DeletePhotoAlbumOperation();
            case PhotoRequestFactory.REQUEST_EDIT_ALBUM:
                return new EditPhotoAlbumOperation();

            default:
                return null;
        }
    }

    public static final String ARGUMENT_NEW_SERVICE_ERROR = "new_service_error";

    @Override
    protected Bundle onCustomRequestException(Request request, CustomRequestException exception) {
        Bundle bundle = super.onCustomRequestException(request, exception);

        if(exception instanceof ServiceException){
            ServiceException serviceException = (ServiceException) exception;
            if(Objects.isNull(bundle)){
                bundle = new Bundle();
            }

            bundle.putBundle(ARGUMENT_NEW_SERVICE_ERROR, serviceException.serializeToBundle());
        }

        return bundle;
    }
}
