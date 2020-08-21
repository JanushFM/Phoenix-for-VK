package biz.dealnote.messenger.adapter;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Transformation;

import java.io.File;
import java.util.List;
import java.util.Objects;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.base.RecyclerBindableAdapter;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.OptionRequest;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.menu.AudioItem;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.PolyTransformation;
import biz.dealnote.messenger.util.RoundTransformation;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.view.WeakViewAnimatorAdapter;

public class LocalAudioRecyclerAdapter extends RecyclerBindableAdapter<Audio, LocalAudioRecyclerAdapter.AudioHolder> {

    private final Context mContext;
    private ClickListener mClickListener;

    public LocalAudioRecyclerAdapter(Context context, List<Audio> data) {
        super(data);
        mContext = context;
    }

    @DrawableRes
    private int getAudioCoverSimple() {
        return Settings.get().main().isAudio_round_icon() ? R.drawable.audio_button : R.drawable.audio_button_material;
    }

    private Transformation TransformCover() {
        return Settings.get().main().isAudio_round_icon() ? new RoundTransformation() : new PolyTransformation();
    }

    private void updateAudioStatus(AudioHolder holder, Audio audio) {
        switch (MusicUtils.AudioStatus(audio)) {
            case 1:
                holder.visual.setVisibility(View.VISIBLE);
                Utils.doAnimateLottie(holder.visual, true, 104);
                holder.play_icon.setVisibility(View.GONE);
                holder.play_cover.setColorFilter(Color.parseColor("#44000000"));
                break;
            case 2:
                holder.visual.setVisibility(View.VISIBLE);
                Utils.doAnimateLottie(holder.visual, false, 104);
                holder.play_icon.setVisibility(View.GONE);
                holder.play_cover.setColorFilter(Color.parseColor("#44000000"));
                break;
            default:
                if (holder.visual.isAnimating()) {
                    holder.visual.cancelAnimation();
                }
                holder.visual.setVisibility(View.GONE);
                holder.play_icon.setVisibility(View.VISIBLE);
                holder.play_icon.setImageResource(Utils.isEmpty(audio.getUrl()) ? R.drawable.audio_died : R.drawable.song);
                holder.play_cover.clearColorFilter();
                break;

        }
    }

    @Override
    protected void onBindItemViewHolder(AudioHolder holder, int position, int type) {
        Audio audio = getItem(position);

        holder.cancelSelectionAnimation();
        if (audio.isAnimationNow()) {
            holder.startSelectionAnimation();
            audio.setAnimationNow(false);
        }

        holder.artist.setText(audio.getArtist());
        holder.title.setText(audio.getTitle());

        updateAudioStatus(holder, audio);

        if (Settings.get().other().isShow_audio_cover()) {
            if (!Utils.isEmpty(audio.getThumb_image_little())) {
                PicassoInstance.with()
                        .load(audio.getThumb_image_little())
                        .placeholder(Objects.requireNonNull(ResourcesCompat.getDrawable(mContext.getResources(), getAudioCoverSimple(), mContext.getTheme())))
                        .transform(TransformCover())
                        .tag(Constants.PICASSO_TAG)
                        .into(holder.play_cover);
            } else {
                PicassoInstance.with().cancelRequest(holder.play_cover);
                holder.play_cover.setImageResource(getAudioCoverSimple());
            }
        } else {
            PicassoInstance.with().cancelRequest(holder.play_cover);
            holder.play_cover.setImageResource(getAudioCoverSimple());
        }

        holder.play.setOnClickListener(v -> {
            if (MusicUtils.isNowPlayingOrPreparingOrPaused(audio)) {
                if (!Settings.get().other().isUse_stop_audio()) {
                    MusicUtils.playOrPause();
                } else {
                    MusicUtils.stop();
                }
            } else {
                if (mClickListener != null) {
                    mClickListener.onClick(position, audio);
                }
            }
        });
        holder.Track.setOnClickListener(view -> {
            holder.cancelSelectionAnimation();
            holder.startSomeAnimation();

            ModalBottomSheetDialogFragment.Builder menus = new ModalBottomSheetDialogFragment.Builder();

            menus.add(new OptionRequest(AudioItem.save_item_audio, mContext.getString(R.string.upload), R.drawable.web));
            menus.add(new OptionRequest(AudioItem.play_item_audio, mContext.getString(R.string.play), R.drawable.play));
            menus.add(new OptionRequest(AudioItem.add_item_audio, mContext.getString(R.string.delete), R.drawable.ic_outline_delete));


            menus.header(Utils.firstNonEmptyString(audio.getArtist(), " ") + " - " + audio.getTitle(), R.drawable.song, audio.getThumb_image_little());
            menus.columns(2);
            menus.show(((FragmentActivity) mContext).getSupportFragmentManager(), "audio_options", option -> {
                switch (option.getId()) {
                    case AudioItem.save_item_audio:
                        if (mClickListener != null) {
                            mClickListener.onUpload(position, audio);
                        }
                        break;
                    case AudioItem.play_item_audio:
                        if (mClickListener != null) {
                            mClickListener.onClick(position, audio);
                            if (Settings.get().other().isShow_mini_player())
                                PlaceFactory.getPlayerPlace(Settings.get().accounts().getCurrent()).tryOpenWith(mContext);
                        }
                        break;
                    case AudioItem.add_item_audio:
                        try {
                            if (new File(Objects.requireNonNull(Uri.parse(audio.getUrl()).getPath())).delete()) {
                                Snackbar.make(view, R.string.success, BaseTransientBottomBar.LENGTH_LONG).show();
                                notifyDataSetChanged();
                            }
                        } catch (Exception e) {
                            PhoenixToast.CreatePhoenixToast(mContext).showToastError(e.getLocalizedMessage());
                        }
                        break;
                    default:
                        break;
                }
            });
        });
    }

    @Override
    protected AudioHolder viewHolder(View view, int type) {
        return new AudioHolder(view);
    }

    @Override
    protected int layoutId(int type) {
        return R.layout.item_local_audio;
    }

    public void setData(List<Audio> data) {
        setItems(data);
    }

    public void setClickListener(ClickListener clickListener) {
        mClickListener = clickListener;
    }

    public interface ClickListener {
        void onClick(int position, Audio audio);

        void onUpload(int position, Audio audio);
    }

    class AudioHolder extends RecyclerView.ViewHolder {

        TextView artist;
        TextView title;
        View play;
        ImageView play_icon;
        ImageView play_cover;
        View Track;
        MaterialCardView selectionView;
        Animator.AnimatorListener animationAdapter;
        ObjectAnimator animator;
        LottieAnimationView visual;

        AudioHolder(View itemView) {
            super(itemView);
            artist = itemView.findViewById(R.id.dialog_title);
            title = itemView.findViewById(R.id.dialog_message);
            play = itemView.findViewById(R.id.item_audio_play);
            play_icon = itemView.findViewById(R.id.item_audio_play_icon);
            play_cover = itemView.findViewById(R.id.item_audio_play_cover);
            Track = itemView.findViewById(R.id.track_option);
            selectionView = itemView.findViewById(R.id.item_audio_selection);
            visual = itemView.findViewById(R.id.item_audio_visual);
            animationAdapter = new WeakViewAnimatorAdapter<View>(selectionView) {
                @Override
                public void onAnimationEnd(View view) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationStart(View view) {
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                protected void onAnimationCancel(View view) {
                    view.setVisibility(View.GONE);
                }
            };
        }

        void startSelectionAnimation() {
            selectionView.setCardBackgroundColor(CurrentTheme.getColorPrimary(mContext));
            selectionView.setAlpha(0.5f);

            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f);
            animator.setDuration(1500);
            animator.addListener(animationAdapter);
            animator.start();
        }

        void startSomeAnimation() {
            selectionView.setCardBackgroundColor(CurrentTheme.getColorSecondary(mContext));
            selectionView.setAlpha(0.5f);

            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f);
            animator.setDuration(500);
            animator.addListener(animationAdapter);
            animator.start();
        }

        void cancelSelectionAnimation() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }

            selectionView.setVisibility(View.INVISIBLE);
        }
    }
}
