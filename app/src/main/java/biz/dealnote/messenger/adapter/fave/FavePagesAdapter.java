package biz.dealnote.messenger.adapter.fave;

import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Transformation;

import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.model.FavePage;
import biz.dealnote.messenger.model.FavePageType;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.AspectRatioImageView;
import biz.dealnote.messenger.view.OnlineView;

public class FavePagesAdapter extends RecyclerView.Adapter<FavePagesAdapter.Holder> {

    private final Context context;
    private final Transformation transformation;
    private List<FavePage> data;
    private RecyclerView recyclerView;
    private ClickListener clickListener;

    public FavePagesAdapter(List<FavePage> data, Context context) {
        this.data = data;
        this.context = context;
        this.transformation = CurrentTheme.createTransformationForAvatar(context);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_fave_page, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, int position) {
        final FavePage favePage = data.get(position);
        holder.description.setText(favePage.getDescription());
        holder.name.setText(favePage.getOwner().getFullName());
        ViewUtils.displayAvatar(holder.avatar, transformation, favePage.getOwner().getMaxSquareAvatar(), Constants.PICASSO_TAG);

        if (favePage.getType().equals(FavePageType.USER)) {
            holder.ivOnline.setVisibility(View.VISIBLE);
            User user = favePage.getUser();
            if (user.getBlacklisted()) {
                holder.blacklisted.setVisibility(View.VISIBLE);
            } else {
                holder.blacklisted.setVisibility(View.GONE);
            }
            Integer onlineIcon = ViewUtils.getOnlineIcon(true, user.isOnlineMobile(), user.getPlatform(), user.getOnlineApp());
            if (!user.isOnline())
                holder.ivOnline.setCircleColor(CurrentTheme.getColorFromAttrs(R.attr.icon_color_inactive, context, "#000000"));
            else
                holder.ivOnline.setCircleColor(CurrentTheme.getColorFromAttrs(R.attr.icon_color_active, context, "#000000"));

            if (onlineIcon != null) {
                holder.ivOnline.setIcon(onlineIcon);
            }
        } else {
            holder.ivOnline.setVisibility(View.GONE);
            holder.blacklisted.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPageClick(holder.getBindingAdapterPosition(), favePage.getOwner());
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(List<FavePage> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface ClickListener {
        void onPageClick(int index, Owner owner);

        void onDelete(int index, Owner owner);
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        AspectRatioImageView avatar;
        ImageView blacklisted;
        TextView name;
        TextView description;
        OnlineView ivOnline;

        public Holder(View itemView) {
            super(itemView);
            itemView.setOnCreateContextMenuListener(this);
            ivOnline = itemView.findViewById(R.id.header_navi_menu_online);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            description = itemView.findViewById(R.id.description);
            blacklisted = itemView.findViewById(R.id.item_blacklisted);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            final int position = recyclerView.getChildAdapterPosition(v);
            final FavePage favePage = data.get(position);
            menu.setHeaderTitle(favePage.getOwner().getFullName());

            menu.add(0, v.getId(), 0, R.string.delete).setOnMenuItemClickListener(item -> {
                if (clickListener != null) {
                    clickListener.onDelete(position, favePage.getOwner());
                }
                return true;
            });
        }
    }
}
