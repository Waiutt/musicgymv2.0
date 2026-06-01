package com.example.musicgym;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// 👉 自动导入 Glide 引擎
import com.bumptech.glide.Glide;

import java.util.List;

public class BlogAdapter extends RecyclerView.Adapter<BlogAdapter.BlogViewHolder> {

    private List<BlogPost> blogList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(BlogPost post);
    }

    public BlogAdapter(List<BlogPost> blogList, OnItemClickListener listener) {
        this.blogList = blogList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BlogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blog_post, parent, false);
        return new BlogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlogViewHolder holder, int position) {
        BlogPost post = blogList.get(position);

        holder.tvTitle.setText(post.getTitle());
        holder.tvAuthor.setText(post.getAuthor());
        holder.tvDate.setText(post.getDate());

        if (post.getImageUri() != null && !post.getImageUri().isEmpty()) {
            holder.ivImage.setVisibility(View.VISIBLE);

            // Glide 极其强大，无论是本地路径、网络链接还是资源ID，它都能自动识别！
            Glide.with(holder.itemView.getContext())
                    .load(post.getImageUri())
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(post));
    }

    @Override
    public int getItemCount() {
        return blogList.size();
    }

    public static class BlogViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor, tvDate;
        ImageView ivImage;

        public BlogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_post_title);
            tvAuthor = itemView.findViewById(R.id.tv_post_author);
            tvDate = itemView.findViewById(R.id.tv_post_date);
            ivImage = itemView.findViewById(R.id.iv_post_image);
        }
    }
}