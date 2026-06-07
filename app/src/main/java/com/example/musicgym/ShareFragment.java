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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private SwipeRefreshLayout swipe;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_share, container, false);

        tvStatus = view.findViewById(R.id.share_status);
        recyclerView = view.findViewById(R.id.rv_blog_posts);
        swipe = view.findViewById(R.id.share_swipe);

        StaggeredGridLayoutManager lm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        lm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        recyclerView.setLayoutManager(lm);

        adapter = new CommunityAdapter(posts, this::openPostDetail);
        recyclerView.setAdapter(adapter);

        repo = new CommunityRepository();
        userManager = UserManager.get(requireContext());

        FloatingActionButton fab = view.findViewById(R.id.fab_add_post);
        fab.setOnClickListener(v -> new CreatePostSheet()
                .show(getParentFragmentManager(), "create_post"));

        // 下拉刷新
        swipe.setOnRefreshListener(this::loadPosts);
        swipe.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light);

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
            if (swipe != null) swipe.setRefreshing(false);
            if (tvStatus == null) return;
            if (result == null) {
                tvStatus.setText("社区不可用，请检查网络连接");
                posts.clear();
                adapter.notifyDataSetChanged();
                return;
            }
            posts.clear();
            posts.addAll(result);
            adapter.notifyDataSetChanged();
            tvStatus.setText(result.isEmpty() ? "暂无帖子，点击 + 发布第一条" : "");
        });
    }

    private void openPostDetail(CommunityRepository.CommunityPost post) {
        Bundle args = new Bundle();
        args.putString("post_id", post.id);
        args.putString("title", post.title);
        args.putString("author", post.nickname);
        args.putLong("timestamp", post.timestamp);
        args.putString("content", post.content);
        args.putString("image_uri", post.imageUri);
        args.putInt("likes", post.likeCount);
        args.putInt("comments", post.commentCount);
        PostDetailSheet sheet = new PostDetailSheet();
        sheet.setArguments(args);
        sheet.show(getParentFragmentManager(), "post_detail");
    }
}
