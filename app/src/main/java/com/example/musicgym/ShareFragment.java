package com.example.musicgym;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShareFragment extends Fragment {

    private RecyclerView recyclerView;
    private BlogAdapter adapter;
    private List<BlogPost> blogList;
    private AppDatabase db;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_share, container, false);

        recyclerView = view.findViewById(R.id.rv_blog_posts);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        staggeredGridLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);

        blogList = new ArrayList<>();
        adapter = new BlogAdapter(blogList, post -> showPostDetail(post));
        recyclerView.setAdapter(adapter);

        db = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // ⚡ 点击悬浮按钮，跃迁到真实的发布页面！
        FloatingActionButton fabAddPost = view.findViewById(R.id.fab_add_post);
        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreatePostActivity.class);
            startActivity(intent);
        });

        return view;
    }

    // ⚡ 核心逻辑：每次回到这个页面 (onResume)，都去硬盘拉取最新数据
    @Override
    public void onResume() {
        super.onResume();
        loadDataFromDatabase();
    }

    private void loadDataFromDatabase() {
        executorService.execute(() -> {
            List<BlogPost> savedPosts = db.blogPostDao().getAllPosts();

            // 如果没数据，我们写入测试数据。
            // ⚡ 骚操作：把 R.drawable.pic1 动态转换成了真实的 String URI！
            if (savedPosts.isEmpty()) {
                String pkgName = requireContext().getPackageName();
                String uri1 = "android.resource://" + pkgName + "/" + R.drawable.pic1;
                String uri2 = "android.resource://" + pkgName + "/" + R.drawable.pic2;
                String uri3 = "android.resource://" + pkgName + "/" + R.drawable.pic3;
                String uri4 = "android.resource://" + pkgName + "/" + R.drawable.pic4;

                db.blogPostDao().insertPost(new BlogPost("Neon Night Run", "2026-04-15", "Hit the neon streets...", "CyberRunner99", "Hit the neon streets tonight.", uri1));
                db.blogPostDao().insertPost(new BlogPost("Deadlift PR", "2026-04-14", "Finally broke the 150kg...", "IronGlitch", "Finally broke the 150kg barrier.", uri2));
                db.blogPostDao().insertPost(new BlogPost("Yoga Protocol", "2026-04-12", "Rest days are just as important...", "ZenHacker", "Rest days are just as important.", uri3));
                db.blogPostDao().insertPost(new BlogPost("Calorie Alg", "2026-04-10", "Wrote a new script...", "ByteLifter", "Wrote a new script to track my macros.", uri4));

                savedPosts = db.blogPostDao().getAllPosts();
            }

            final List<BlogPost> finalPosts = savedPosts;
            mainHandler.post(() -> {
                blogList.clear();
                blogList.addAll(finalPosts);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void showPostDetail(BlogPost post) {
        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
        intent.putExtra("POST_TITLE", post.getTitle());
        intent.putExtra("POST_AUTHOR", post.getAuthor());
        intent.putExtra("POST_DATE", post.getDate());
        intent.putExtra("POST_CONTENT", post.getFullContent());

        // ⚡ 传递新版 String 类型的图片路径
        intent.putExtra("POST_IMAGE_URI", post.getImageUri());
        startActivity(intent);
    }
}