package com.example.musicgym;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Firestore 社区数据层 — 帖子/点赞/评论 */
public class CommunityRepository {

    private final FirebaseFirestore db;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public CommunityRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // ═══════════ 帖子 ═══════════

    /** 发布帖子 */
    public void publishPost(String userId, String nickname, String title,
                             String content, String imageUri,
                             OnCompleteListener<String> listener) {
        Map<String, Object> post = new HashMap<>();
        post.put("userId", userId);
        post.put("nickname", nickname);
        post.put("title", title);
        post.put("content", content);
        post.put("imageUri", imageUri != null ? imageUri : "");
        post.put("timestamp", System.currentTimeMillis());
        post.put("likes", new ArrayList<String>());
        post.put("comments", new ArrayList<Map<String, Object>>());

        db.collection("posts").add(post)
                .addOnSuccessListener(doc -> {
                    if (listener != null) listener.onResult(doc.getId());
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onResult(null);
                });
    }

    /** 加载帖子列表（按时间倒序） */
    public void loadPosts(OnPostsLoadedListener listener) {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnCompleteListener(task -> {
                    List<CommunityPost> result = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            result.add(docToPost(doc));
                        }
                    }
                    if (listener != null) listener.onLoaded(result);
                });
    }

    /** 加载单个帖子 */
    public void loadPost(String postId, OnPostLoadedListener listener) {
        db.collection("posts").document(postId).get()
                .addOnCompleteListener(task -> {
                    CommunityPost post = null;
                    if (task.isSuccessful() && task.getResult() != null) {
                        post = docToPost(task.getResult());
                    }
                    if (listener != null) listener.onLoaded(post);
                });
    }

    // ═══════════ 点赞 ═══════════

    /** 切换点赞状态 */
    public void toggleLike(String postId, String userId, OnCompleteListener<Boolean> listener) {
        db.collection("posts").document(postId).get()
                .addOnSuccessListener(doc -> {
                    List<String> likes = (List<String>) doc.get("likes");
                    if (likes == null) likes = new ArrayList<>();
                    boolean isLiked = likes.contains(userId);

                    if (isLiked) {
                        db.collection("posts").document(postId)
                                .update("likes", FieldValue.arrayRemove(userId));
                    } else {
                        db.collection("posts").document(postId)
                                .update("likes", FieldValue.arrayUnion(userId));
                    }
                    if (listener != null) listener.onResult(!isLiked);
                });
    }

    // ═══════════ 评论 ═══════════

    /** 添加评论 */
    public void addComment(String postId, String userId, String nickname,
                            String text, OnCompleteListener<Boolean> listener) {
        Map<String, Object> comment = new HashMap<>();
        comment.put("userId", userId);
        comment.put("nickname", nickname);
        comment.put("text", text);
        comment.put("timestamp", System.currentTimeMillis());

        db.collection("posts").document(postId)
                .update("comments", FieldValue.arrayUnion(comment))
                .addOnSuccessListener(v -> {
                    if (listener != null) listener.onResult(true);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onResult(false);
                });
    }

    // ═══════════ 工具 ═══════════

    private CommunityPost docToPost(DocumentSnapshot doc) {
        CommunityPost p = new CommunityPost();
        p.id = doc.getId();
        p.userId = doc.getString("userId");
        p.nickname = doc.getString("nickname");
        p.title = doc.getString("title");
        p.content = doc.getString("content");
        p.imageUri = doc.getString("imageUri");
        Long ts = doc.getLong("timestamp");
        p.timestamp = ts != null ? ts : 0;

        List<String> likes = (List<String>) doc.get("likes");
        p.likeCount = likes != null ? likes.size() : 0;

        List<Map<String, Object>> comments = (List<Map<String, Object>>) doc.get("comments");
        p.commentCount = comments != null ? comments.size() : 0;
        p.comments = comments != null ? comments : new ArrayList<>();

        return p;
    }

    // ═══════════ 数据容器 ═══════════

    public static class CommunityPost {
        public String id, userId, nickname, title, content, imageUri;
        public long timestamp;
        public int likeCount, commentCount;
        public List<Map<String, Object>> comments;
    }

    public interface OnCompleteListener<T> { void onResult(T result); }

    public interface OnPostsLoadedListener { void onLoaded(List<CommunityPost> posts); }

    public interface OnPostLoadedListener { void onLoaded(CommunityPost post); }
}
