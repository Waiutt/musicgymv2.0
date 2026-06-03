package com.example.musicgym;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/** 社区 Tab — Firebase Firestore 驱动的真实社交平台 */
public class ShareFragment extends Fragment {

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;
    private final List<CommunityRepository.CommunityPost> posts = new ArrayList<>();
    private CommunityRepository repo;
    private UserManager userManager;
    private TextView tvStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_share, container, false);

        tvStatus = view.findViewById(R.id.share_status);
        recyclerView = view.findViewById(R.id.rv_blog_posts);
        StaggeredGridLayoutManager lm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        lm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        recyclerView.setLayoutManager(lm);

        adapter = new CommunityAdapter(posts, this::openPostDetail);
        recyclerView.setAdapter(adapter);

        repo = new CommunityRepository();
        userManager = UserManager.get(requireContext());

        FloatingActionButton fab = view.findViewById(R.id.fab_add_post);
        fab.setOnClickListener(v -> {
            if (userManager.getUserId() == null) {
                Toast.makeText(getContext(), "正在连接社区...", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(requireContext(), CreatePostActivity.class));
        });

        // 登录后加载数据
        userManager.signIn((userId, nickname) -> loadPosts());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userManager.getUserId() != null) loadPosts();
    }

    private void loadPosts() {
        if (tvStatus != null) tvStatus.setText("加载中...");
        repo.loadPosts(result -> {
            posts.clear();
            posts.addAll(result);
            adapter.notifyDataSetChanged();
            if (tvStatus != null) {
                tvStatus.setText(result.isEmpty() ? "暂无帖子，点击 + 发布第一条" : "");
            }
        });
    }

    private void openPostDetail(CommunityRepository.CommunityPost post) {
        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
        intent.putExtra("POST_ID", post.id);
        intent.putExtra("POST_TITLE", post.title);
        intent.putExtra("POST_AUTHOR", post.nickname);
        intent.putExtra("POST_DATE", String.valueOf(post.timestamp));
        intent.putExtra("POST_CONTENT", post.content);
        intent.putExtra("POST_IMAGE_URI", post.imageUri);
        intent.putExtra("POST_LIKES", post.likeCount);
        intent.putExtra("POST_COMMENTS", post.commentCount);
        startActivity(intent);
    }
}
