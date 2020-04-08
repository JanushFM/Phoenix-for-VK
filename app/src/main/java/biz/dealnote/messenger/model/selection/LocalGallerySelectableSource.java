package biz.dealnote.messenger.model.selection;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Ruslan Kolbasa on 16.08.2017.
 * phoenix
 */
public class LocalGallerySelectableSource extends AbsSelectableSource implements Parcelable {

    public LocalGallerySelectableSource() {
        super(Types.LOCAL_GALLERY);
    }

    protected LocalGallerySelectableSource(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static final Creator<LocalGallerySelectableSource> CREATOR = new Creator<LocalGallerySelectableSource>() {
        @Override
        public LocalGallerySelectableSource createFromParcel(Parcel in) {
            return new LocalGallerySelectableSource(in);
        }

        @Override
        public LocalGallerySelectableSource[] newArray(int size) {
            return new LocalGallerySelectableSource[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
