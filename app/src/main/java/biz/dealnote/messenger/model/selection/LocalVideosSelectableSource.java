package biz.dealnote.messenger.model.selection;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Ruslan Kolbasa on 16.08.2017.
 * phoenix
 */
public class LocalVideosSelectableSource extends AbsSelectableSource implements Parcelable {

    public LocalVideosSelectableSource() {
        super(Types.VIDEOS);
    }

    protected LocalVideosSelectableSource(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static final Creator<LocalVideosSelectableSource> CREATOR = new Creator<LocalVideosSelectableSource>() {
        @Override
        public LocalVideosSelectableSource createFromParcel(Parcel in) {
            return new LocalVideosSelectableSource(in);
        }

        @Override
        public LocalVideosSelectableSource[] newArray(int size) {
            return new LocalVideosSelectableSource[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
