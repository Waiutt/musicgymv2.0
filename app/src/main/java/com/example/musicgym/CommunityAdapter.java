package com.example.musicgym;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 社区帖子瀑布流适配器 — 接入 Firestore 真实数据 */
public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    private final List<CommunityRepository.CommunityPost> posts;
    private final OnPostClickListener listener;

    public interface OnPostClickListener {
        void onClick(CommunityRepository.CommunityPost post);
    }

    public CommunityAdapter(List<CommunityRepository.CommunityPost> posts, OnPostClickListener listener) {
        this.posts = posts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blog_post, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityRepository.CommunityPost post = posts.get(position);

        holder.tvTitle.setText(post.title != null ? post.title : "");
        holder.tvAuthor.setText(post.nickname != null ? post.nickname : "匿名");

        if (post.timestamp > 0) {
            holder.tvDate.setText(new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    .format(new Date(post.timestamp)));
        } else {
            holder.tvDate.setText("");
        }

        if (post.imageUri != null && !post.imageUri.isEmpty()) {
            holder.ivImage.setVisibility(View.VISIBLE);
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(post.imageUri)
                    .placeholder(R.drawable.pic1)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }

        holder.tvStats.setText("❤ " + post.likeCount + "  💬 " + post.commentCount);

        // 点击帖子 → 详情
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(post);
        });

        // 点击作者 → 用户主页
        holder.tvAuthor.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), UserProfileActivity.class);
            i.putExtra("user_id", post.userId);
            i.putExtra("nickname", post.nickname);
            v.getContext().startActivity(i);
        });

        // 长按 → 举报
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("举报帖子")
                    .setMessage("确定举报该帖子？")
                    .setPositiveButton("举报", (d, w) -> {
                        UserManager.get(v.getContext()).signIn((uid, nn) ->
                                new CommunityRepository().reportPost(post.id, uid, "用户举报"));
                        Toast.makeText(v.getContext(), "已提交举报", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() { return posts.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvAuthor, tvDate, tvStats;

        ViewHolder(View v) {
            super(v);
            ivImage = v.findViewById(R.id.iv_post_image);
            tvTitle = v.findViewById(R.id.tv_post_title);
            tvAuthor = v.findViewById(R.id.tv_post_author);
            tvDate = v.findViewById(R.id.tv_post_date);
            tvStats = v.findViewById(R.id.tv_post_stats);
        }
    }
}
