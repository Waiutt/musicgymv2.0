package com.example.musicgym;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {

    private CommunityRepository repo;
    private UserManager userManager;
    private String postId;

    private TextView tvTitle, tvAuthor, tvDate, tvContent, tvLikes, tvComments;
    private ImageView ivImage;
    private View btnLike, btnComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        ivImage = findViewById(R.id.detail_iv_image);
        tvTitle = findViewById(R.id.detail_tv_title);
        tvAuthor = findViewById(R.id.detail_tv_author);
        tvDate = findViewById(R.id.detail_tv_date);
        tvContent = findViewById(R.id.detail_tv_content);
        tvLikes = findViewById(R.id.detail_tv_likes);
        tvComments = findViewById(R.id.detail_tv_comments);
        btnLike = findViewById(R.id.detail_btn_like);
        btnComment = findViewById(R.id.detail_btn_comment);
        ImageButton btnBack = findViewById(R.id.detail_btn_back);
        btnBack.setOnClickListener(v -> finish());

        repo = new CommunityRepository();
        userManager = UserManager.get(this);
        userManager.signIn((uid, nick) -> loadPost());

        postId = getIntent().getStringExtra("POST_ID");

        // 降级：如果没有 Firestore postId，显示旧版本地数据
        String title = getIntent().getStringExtra("POST_TITLE");
        if (postId == null && title != null) {
            showLocalData(title);
            return;
        }

        btnLike.setOnClickListener(v -> toggleLike());
        btnComment.setOnClickListener(v -> addComment());
    }

    private void showLocalData(String title) {
        tvTitle.setText(title);
        tvAuthor.setText(getIntent().getStringExtra("POST_AUTHOR"));
        tvDate.setText(getIntent().getStringExtra("POST_DATE"));
        tvContent.setText(getIntent().getStringExtra("POST_CONTENT"));
        String uri = getIntent().getStringExtra("POST_IMAGE_URI");
        if (uri != null && !uri.isEmpty()) Glide.with(this).load(uri).into(ivImage);
        tvLikes.setVisibility(View.GONE);
        tvComments.setVisibility(View.GONE);
        btnLike.setVisibility(View.GONE);
        btnComment.setVisibility(View.GONE);
    }

    private void loadPost() {
        if (postId == null) return;
        repo.loadPost(postId, post -> {
            if (post == null) return;
            tvTitle.setText(post.title);
            tvAuthor.setText(post.nickname);
            tvDate.setText(post.timestamp > 0
                    ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(post.timestamp))
                    : "");
            tvContent.setText(post.content);
            tvLikes.setText("❤ " + post.likeCount);
            tvComments.setText("💬 " + post.commentCount);

            if (post.imageUri != null && !post.imageUri.isEmpty()) {
                Glide.with(this).load(post.imageUri).into(ivImage);
            }
        });
    }

    private void toggleLike() {
        if (postId == null || userManager.getUserId() == null) return;
        repo.toggleLike(postId, userManager.getUserId(), liked -> {
            Toast.makeText(this, liked ? "已点赞 ❤" : "已取消点赞", Toast.LENGTH_SHORT).show();
            loadPost();
        });
    }

    private void addComment() {
        if (postId == null || userManager.getUserId() == null) return;
        EditText et = new EditText(this);
        et.setHint("写评论...");
        new AlertDialog.Builder(this)
                .setTitle("添加评论")
                .setView(et)
                .setPositiveButton("发送", (d, w) -> {
                    String text = et.getText().toString().trim();
                    if (!text.isEmpty()) {
                        repo.addComment(postId, userManager.getUserId(),
                                userManager.getNickname(), text, ok -> {
                                    Toast.makeText(this, "评论已发送", Toast.LENGTH_SHORT).show();
                                    loadPost();
                                });
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
