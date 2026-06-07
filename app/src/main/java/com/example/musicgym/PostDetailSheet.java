package com.example.musicgym;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 帖子详情浮窗 — 替代 PostDetailActivity */
public class PostDetailSheet extends BottomSheetDialogFragment {

    private CommunityRepository repo;
    private UserManager userManager;
    private String postId;
    private int likeCount, commentCount;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_MusicGym);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sheet_post_detail, container, false);

        Bundle args = getArguments();
        if (args == null) { dismiss(); return v; }

        postId = args.getString("post_id", "");

        TextView tvTitle = v.findViewById(R.id.detail_tv_title);
        TextView tvAuthor = v.findViewById(R.id.detail_tv_author);
        TextView tvDate = v.findViewById(R.id.detail_tv_date);
        TextView tvContent = v.findViewById(R.id.detail_tv_content);
        TextView tvLikes = v.findViewById(R.id.detail_tv_likes);
        ImageView ivImage = v.findViewById(R.id.detail_iv_image);
        View btnLike = v.findViewById(R.id.detail_btn_like);
        View btnComment = v.findViewById(R.id.detail_btn_comment);

        repo = new CommunityRepository();
        userManager = UserManager.get(requireContext());
        userManager.signIn((uid, nick) -> {});

        // 降级模式：直接显示传入的数据
        tvTitle.setText(args.getString("title", ""));
        tvAuthor.setText(args.getString("author", ""));
        long ts = args.getLong("timestamp", 0);
        tvDate.setText(ts > 0 ? new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                .format(new Date(ts)) : "");
        tvContent.setText(args.getString("content", ""));
        likeCount = args.getInt("likes", 0);
        commentCount = args.getInt("comments", 0);
        tvLikes.setText("❤ " + likeCount + "  💬 " + commentCount);
        String imgUri = args.getString("image_uri", "");
        if (!imgUri.isEmpty()) Glide.with(this).load(imgUri).into(ivImage);

        // 如果有 Firebase postId，从服务器加载最新数据
        if (!postId.isEmpty()) {
            repo.loadPost(postId, post -> {
                if (post != null) {
                    tvTitle.setText(post.title);
                    tvAuthor.setText(post.nickname);
                    tvContent.setText(post.content);
                    likeCount = post.likeCount;
                    commentCount = post.commentCount;
                    tvLikes.setText("❤ " + likeCount + "  💬 " + commentCount);
                }
            });
        }

        btnLike.setOnClickListener(v2 -> {
            if (postId.isEmpty()) return;
            repo.toggleLike(postId, userManager.getUserId(), liked -> {
                Toast.makeText(getContext(), liked ? "已点赞 ❤" : "已取消", Toast.LENGTH_SHORT).show();
                likeCount += liked ? 1 : -1;
                tvLikes.setText("❤ " + likeCount + "  💬 " + commentCount);
            });
        });

        btnComment.setOnClickListener(v2 -> {
            if (postId.isEmpty()) return;
            EditText et = new EditText(getContext());
            et.setHint("写评论...");
            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("评论").setView(et)
                    .setPositiveButton("发送", (d, w) -> {
                        String text = et.getText().toString().trim();
                        if (!text.isEmpty()) {
                            repo.addComment(postId, userManager.getUserId(),
                                    userManager.getNickname(), text, ok -> {
                                        commentCount++;
                                        tvLikes.setText("❤ " + likeCount + "  💬 " + commentCount);
                                    });
                        }
                    }).setNegativeButton("取消", null).show();
        });

        return v;
    }
}
