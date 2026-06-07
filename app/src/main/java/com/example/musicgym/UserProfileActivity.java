package com.example.musicgym;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;

/** 用户主页 — 查看他人运动统计 + 发帖历史 */
public class UserProfileActivity extends AppCompatActivity {

    private CommunityRepository repo;
    private final ArrayList<CommunityRepository.CommunityPost> posts = new ArrayList<>();
    private CommunityAdapter adapter;
    private TextView tvName, tvStats;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        tvName = findViewById(R.id.up_name);
        tvStats = findViewById(R.id.up_stats);
        RecyclerView rv = findViewById(R.id.up_posts);
        TextView btnBack = findViewById(R.id.up_back);

        btnBack.setOnClickListener(v -> finish());

        repo = new CommunityRepository();

        StaggeredGridLayoutManager lm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        lm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        rv.setLayoutManager(lm);
        adapter = new CommunityAdapter(posts, post -> { /* no-op in profile */ });
        rv.setAdapter(adapter);

        String userId = getIntent().getStringExtra("user_id");
        String nickname = getIntent().getStringExtra("nickname");

        tvName.setText(nickname != null ? nickname : "用户");

        if (userId != null) {
            repo.loadUserPosts(userId, result -> {
                if (result != null && !result.isEmpty()) {
                    posts.clear();
                    posts.addAll(result);
                    adapter.notifyDataSetChanged();
                    tvStats.setText(result.size() + " 条动态");
                } else {
                    tvStats.setText("暂无动态");
                }
            });
        }
    }
}
