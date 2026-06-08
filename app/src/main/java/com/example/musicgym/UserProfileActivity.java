package com.example.musicgym;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;

/** 用户主页 — 查看他人运动统计 + 发帖历史 + 关注 */
public class UserProfileActivity extends AppCompatActivity {

    private CommunityRepository repo;
    private UserManager userManager;
    private final ArrayList<CommunityRepository.CommunityPost> posts = new ArrayList<>();
    private CommunityAdapter adapter;
    private TextView tvName, tvStats, btnFollow;

    private String profileUserId, myUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        tvName = findViewById(R.id.up_name);
        tvStats = findViewById(R.id.up_stats);
        btnFollow = findViewById(R.id.up_follow);
        RecyclerView rv = findViewById(R.id.up_posts);
        TextView btnBack = findViewById(R.id.up_back);

        btnBack.setOnClickListener(v -> finish());

        repo = new CommunityRepository();
        userManager = UserManager.get(this);

        StaggeredGridLayoutManager lm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        lm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        rv.setLayoutManager(lm);
        adapter = new CommunityAdapter(posts, post -> { /* no-op in profile */ });
        rv.setAdapter(adapter);

        profileUserId = getIntent().getStringExtra("user_id");
        String nickname = getIntent().getStringExtra("nickname");

        tvName.setText(nickname != null ? nickname : "用户");

        userManager.signIn((uid, nn) -> {
            myUserId = uid;
            if (profileUserId != null) {
                loadUserData();
                checkFollowState();
            }
        });
    }

    private void loadUserData() {
        if (profileUserId == null) return;
        repo.loadUserPosts(profileUserId, result -> {
            if (result != null && !result.isEmpty()) {
                posts.clear();
                posts.addAll(result);
                adapter.notifyDataSetChanged();
            }
            int postCount = result != null ? result.size() : 0;
            repo.getFollowerCount(profileUserId, fc ->
                    runOnUiThread(() -> tvStats.setText(postCount + " 动态 · " + fc + " 粉丝")));
        });
    }

    private void checkFollowState() {
        if (myUserId == null || profileUserId == null || myUserId.equals(profileUserId)) {
            btnFollow.setVisibility(View.GONE);
            return;
        }
        repo.isFollowing(myUserId, profileUserId, isF -> runOnUiThread(() -> {
            if (isF) {
                btnFollow.setText("✓ 已关注");
                btnFollow.setBackgroundColor(ColorTokens.ACCENT_GREEN);
            } else {
                btnFollow.setText("+ 关注");
                btnFollow.setBackgroundColor(ColorTokens.BRAND_ORANGE);
            }
            btnFollow.setOnClickListener(v -> {
                if (isF) {
                    repo.unfollowUser(myUserId, profileUserId);
                    btnFollow.setText("+ 关注");
                    btnFollow.setBackgroundColor(ColorTokens.BRAND_ORANGE);
                    Toast.makeText(this, "已取消关注", Toast.LENGTH_SHORT).show();
                } else {
                    userManager.signIn((un, nn) ->
                            repo.followUser(myUserId, profileUserId, nn));
                    btnFollow.setText("✓ 已关注");
                    btnFollow.setBackgroundColor(ColorTokens.ACCENT_GREEN);
                    Toast.makeText(this, "已关注", Toast.LENGTH_SHORT).show();
                }
                // Force re-check
                checkFollowState();
            });
        }));
    }
}
