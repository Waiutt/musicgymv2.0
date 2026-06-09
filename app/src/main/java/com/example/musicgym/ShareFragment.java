package com.example.musicgym;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 社区 Tab — Firebase Firestore 驱动的真实社交平台 */
public class ShareFragment extends Fragment {

    private ShareViewModel vm;
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
        fab.setOnLongClickListener(v -> {
            showCreateChallengeDialog();
            return true;
        });
        fab.setOnClickListener(v -> new CreatePostSheet()
                .show(getParentFragmentManager(), "create_post"));

        // 挑战按钮
        view.findViewById(R.id.share_challenges).setOnClickListener(v ->
                showChallengesDialog());

        swipe.setOnRefreshListener(() -> vm.loadPosts());
        swipe.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light);

        // ViewModel 观察（仅帖子列表加载状态）
        vm = new ViewModelProvider(this).get(ShareViewModel.class);
        vm.getPosts().observe(getViewLifecycleOwner(), result -> {
            if (swipe != null) swipe.setRefreshing(false);
            posts.clear();
            if (result != null) posts.addAll(result);
            adapter.notifyDataSetChanged();
        });
        vm.getStatusMessage().observe(getViewLifecycleOwner(), msg -> {
            if (tvStatus != null) tvStatus.setText(msg);
        });

        userManager.signIn((userId, nickname) -> vm.loadPosts());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userManager.getUserId() != null) vm.loadPosts();
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

    // ═══════════ 挑战系统 ═══════════

    private void showCreateChallengeDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        EditText etTitle = new EditText(getContext());
        etTitle.setHint("挑战标题（如：本周跑50km）");
        etTitle.setTextColor(Color.WHITE);
        etTitle.setHintTextColor(ColorTokens.TEXT_HINT);
        layout.addView(etTitle);

        EditText etGoal = new EditText(getContext());
        etGoal.setHint("目标值（如：50）");
        etGoal.setTextColor(Color.WHITE);
        etGoal.setHintTextColor(ColorTokens.TEXT_HINT);
        etGoal.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etGoal);

        String[] types = {"🏃 跑步 km", "🚴 骑行 km", "🏋️ 训练次数", "🔥 卡路里"};
        TextView tvType = new TextView(getContext());
        tvType.setText(types[0]);
        tvType.setTextColor(ColorTokens.ACCENT_CYAN);
        tvType.setPadding(0, 12, 0, 0);
        layout.addView(tvType);

        new AlertDialog.Builder(requireContext())
                .setTitle("发起挑战")
                .setView(layout)
                .setPositiveButton("发起", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String goalStr = etGoal.getText().toString().trim();
                    if (title.isEmpty() || goalStr.isEmpty()) return;
                    double goal = Double.parseDouble(goalStr);
                    long endDate = System.currentTimeMillis()
                            + 7L * 24 * 3600 * 1000; // 7天
                    userManager.signIn((uid, nick) ->
                            repo.createChallenge(uid, nick, title, "跑步 km", goal, endDate, result -> {
                                if (result != null)
                                    Toast.makeText(getContext(), "挑战已发起！", Toast.LENGTH_SHORT).show();
                            }));
                })
                .setNegativeButton("取消", null).show();
    }

    private void showChallengesDialog() {
        repo.loadChallenges(challenges -> {
            if (!isAdded() || challenges.isEmpty()) {
                safePost(() -> Toast.makeText(getContext(),
                        "暂无进行中的挑战", Toast.LENGTH_SHORT).show());
                return;
            }
            safePost(() -> {
                String[] items = new String[challenges.size()];
                for (int i = 0; i < challenges.size(); i++) {
                    CommunityRepository.Challenge c = challenges.get(i);
                    int pCount = c.participants != null ? c.participants.size() : 0;
                    items[i] = "🏆 " + c.title + " (" + pCount + "人参与)";
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("进行中的挑战")
                        .setItems(items, (d, w) -> showChallengeDetail(challenges.get(w)))
                        .setNegativeButton("关闭", null).show();
            });
        });
    }

    private void showChallengeDetail(CommunityRepository.Challenge ch) {
        StringBuilder sb = new StringBuilder();
        sb.append("发起人: ").append(ch.creatorName).append("\n");
        sb.append("目标: ").append(ch.goalValue).append(" ").append(ch.goalType).append("\n\n");
        sb.append("🏅 排行榜:\n");
        List<Map<String, Object>> parts = ch.participants;
        if (parts != null) {
            parts.sort((a, b) -> Double.compare(
                    ((Number) b.get("progress")).doubleValue(),
                    ((Number) a.get("progress")).doubleValue()));
            for (int i = 0; i < parts.size(); i++) {
                Map<String, Object> p = parts.get(i);
                Object progress = p.get("progress");
                double prog = progress instanceof Number ? ((Number) progress).doubleValue() : 0;
                sb.append(i + 1).append(". ").append(p.get("nickname"))
                        .append(" — ").append(String.format(Locale.getDefault(), "%.1f", prog))
                        .append(" ").append(ch.goalType).append("\n");
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(ch.title)
                .setMessage(sb.toString())
                .setPositiveButton("加入挑战", (d, w) -> userManager.signIn((uid, nick) -> {
                    repo.joinChallenge(ch.id, uid, nick);
                    Toast.makeText(getContext(), "已加入！完成运动后自动更新进度",
                            Toast.LENGTH_SHORT).show();
                }))
                .setNegativeButton("关闭", null).show();
    }

    private void safePost(Runnable r) {
        if (isAdded() && getActivity() != null) getActivity().runOnUiThread(r);
    }
}
