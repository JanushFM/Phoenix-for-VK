package biz.dealnote.messenger.mvp.view;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.model.EditingPostType;
import biz.dealnote.messenger.model.LoadMoreState;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.Story;
import biz.dealnote.messenger.mvp.view.base.IAccountDependencyView;
import biz.dealnote.mvp.core.IMvpView;

public interface IWallView extends IAttachmentsPlacesView,
        IAccountDependencyView, IMvpView, ISnackbarView, IErrorView, IToastView {

    void displayWallData(List<Post> data);

    void notifyWallDataSetChanged();

    void updateStory(List<Story> stories);

    void notifyWallItemChanged(int position);

    void notifyWallDataAdded(int position, int count);

    void setupLoadMoreFooter(@LoadMoreState int state);

    void showRefreshing(boolean refreshing);

    void openPhotoAlbums(int accountId, int ownerId, @Nullable Owner owner);

    void openAudios(int accountId, int ownerId, @Nullable Owner owner);

    void openVideosLibrary(int accountId, int ownerId, @Nullable Owner owner);

    void goToPostCreation(int accountId, int ownerId, @EditingPostType int postType);

    void copyToClipboard(String label, String body);

    void openPhotoAlbum(int accountId, int ownerId, int albumId, ArrayList<Photo> photos, int position);

    void goToWallSearch(int accountId, int ownerId);

    void openPostEditor(int accountId, Post post);

    void notifyWallItemRemoved(int index);

    void goToConversationAttachments(int accountId, int ownerId);

    interface IOptionView {
        void setIsMy(boolean my);

        void setIsDebug(boolean debug);
    }
}