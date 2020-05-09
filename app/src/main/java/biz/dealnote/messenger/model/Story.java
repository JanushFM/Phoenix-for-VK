package biz.dealnote.messenger.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by admin on 04.12.2016.
 * phoenix
 */
public class Story extends AbsModel implements Parcelable {
    public static final Creator<Story> CREATOR = new Creator<Story>() {
        @Override
        public Story createFromParcel(Parcel in) {
            return new Story(in);
        }

        @Override
        public Story[] newArray(int size) {
            return new Story[size];
        }
    };
    private int id;
    private int owner_id;
    private long date;
    private long expires_at;
    private Photo photo;
    private Video video;
    private Owner author;

    public Story() {

    }

    protected Story(Parcel in) {
        super(in);
        id = in.readInt();
        owner_id = in.readInt();
        date = in.readLong();
        expires_at = in.readLong();
        video = in.readParcelable(Video.class.getClassLoader());
        photo = in.readParcelable(Photo.class.getClassLoader());
        author = ParcelableOwnerWrapper.readOwner(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(id);
        dest.writeInt(owner_id);
        dest.writeLong(date);
        dest.writeLong(expires_at);
        dest.writeParcelable(video, flags);
        dest.writeParcelable(photo, flags);
        ParcelableOwnerWrapper.writeOwner(dest, flags, author);
    }

    public Photo getPhoto() {
        return photo;
    }

    public Story setPhoto(Photo photo) {
        this.photo = photo;
        return this;
    }

    public int getId() {
        return id;
    }

    public Story setId(int id) {
        this.id = id;
        return this;
    }

    public Video getVideo() {
        return video;
    }

    public Story setVideo(Video video) {
        this.video = video;
        return this;
    }

    public int getOwnerId() {
        return owner_id;
    }

    public Story setOwnerId(int ownerId) {
        this.owner_id = ownerId;
        return this;
    }

    public long getDate() {
        return date;
    }

    public Story setDate(long date) {
        this.date = date;
        return this;
    }

    public long getExpires() {
        return expires_at;
    }

    public Story setExpires(long expires_at) {
        this.expires_at = expires_at;
        return this;
    }

    public Owner getOwner() {
        return author;
    }

    public Story setOwner(Owner author) {
        this.author = author;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Story))
            return false;

        Story story = (Story) o;
        return id == story.id && owner_id == story.owner_id;
    }
}
