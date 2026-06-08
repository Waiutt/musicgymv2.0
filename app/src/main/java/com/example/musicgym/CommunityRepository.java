package com.example.musicgym;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Firestore 社区数据层。Firebase 不可用时所有操作静默降级 */
public class CommunityRepository {

    private FirebaseFirestore db;
    private boolean available;

    public CommunityRepository() {
        try {
            db = FirebaseFirestore.getInstance();
            available = true;
        } catch (Exception e) {
            available = false;
        }
    }

    public boolean isAvailable() { return available; }

    // ═══════════ 帖子 ═══════════

    public void publishPost(String userId, String nickname, String title,
                             String content, String imageUri,
                             OnCompleteListener<String> listener) {
        if (!available) { if (listener != null) listener.onResult("local_offline"); return; }
        try {
            Map<String, Object> post = new HashMap<>();
            post.put("userId", userId); post.put("nickname", nickname);
            post.put("title", title); post.put("content", content);
            post.put("imageUri", imageUri != null ? imageUri : "");
            post.put("timestamp", System.currentTimeMillis());
            post.put("likes", new ArrayList<String>());
            post.put("comments", new ArrayList<Map<String, Object>>());
            db.collection("posts").add(post)
                    .addOnSuccessListener(doc -> { if (listener != null) listener.onResult(doc.getId()); })
                    .addOnFailureListener(e -> { if (listener != null) listener.onResult(null); });
        } catch (Exception e) { if (listener != null) listener.onResult(null); }
    }

    public void loadPosts(OnPostsLoadedListener listener) {
        if (!available) {
            // Firebase 不可用 → 从 Room 加载本地帖子
            loadLocalPosts(listener);
            return;
        }
        try {
            db.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get()
                    .addOnCompleteListener(task -> {
                        List<CommunityPost> result = new ArrayList<>();
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (DocumentSnapshot doc : task.getResult()) result.add(docToPost(doc));
                        }
                        if (listener != null) {
                            if (!task.isSuccessful()) listener.onLoaded(null);
                            else if (result.isEmpty()) loadLocalPosts(listener); // Firestore空了，试试本地
                            else listener.onLoaded(result);
                        }
                    });
        } catch (Exception e) { if (listener != null) listener.onLoaded(null); }
    }

    private void loadLocalPosts(OnPostsLoadedListener listener) {
        try {
            android.content.Context ctx = com.example.musicgym.MusicGymApp.getContext();
            if (ctx == null) { listener.onLoaded(new ArrayList<>()); return; }
            AppDatabase localDb = AppDatabase.getInstance(ctx);
            List<BlogPost> local = localDb.blogPostDao().getAllPosts();
            if (local == null || local.isEmpty()) {
                if (listener != null) listener.onLoaded(new ArrayList<>());
                return;
            }
            List<CommunityPost> result = new ArrayList<>();
            for (BlogPost bp : local) result.add(localToPost(bp));
            if (listener != null) listener.onLoaded(result);
        } catch (Exception e) {
            if (listener != null) listener.onLoaded(new ArrayList<>());
        }
    }

    private CommunityPost localToPost(BlogPost bp) {
        CommunityPost p = new CommunityPost();
        p.id = String.valueOf(bp.getId());
        p.userId = "local";
        p.nickname = bp.getAuthor();
        p.title = bp.getTitle();
        p.content = bp.getFullContent();
        p.imageUri = bp.getImageUri();
        p.timestamp = System.currentTimeMillis();
        p.likeCount = 0;
        p.commentCount = 0;
        p.comments = new ArrayList<>();
        return p;
    }

    public void loadPost(String postId, OnPostLoadedListener listener) {
        if (!available) { if (listener != null) listener.onLoaded(null); return; }
        try {
            db.collection("posts").document(postId).get()
                    .addOnCompleteListener(task -> {
                        CommunityPost post = null;
                        if (task.isSuccessful() && task.getResult() != null) post = docToPost(task.getResult());
                        if (listener != null) listener.onLoaded(post);
                    });
        } catch (Exception e) { if (listener != null) listener.onLoaded(null); }
    }

    // ═══════════ 点赞 ═══════════

    public void toggleLike(String postId, String userId, OnCompleteListener<Boolean> listener) {
        if (!available) return;
        try {
            db.collection("posts").document(postId).get().addOnSuccessListener(doc -> {
                List<String> likes = (List<String>) doc.get("likes");
                if (likes == null) likes = new ArrayList<>();
                if (likes.contains(userId))
                    db.collection("posts").document(postId).update("likes", FieldValue.arrayRemove(userId));
                else
                    db.collection("posts").document(postId).update("likes", FieldValue.arrayUnion(userId));
                if (listener != null) listener.onResult(!likes.contains(userId));
            });
        } catch (Exception ignored) {}
    }

    // ═══════════ 评论 ═══════════

    public void addComment(String postId, String userId, String nickname, String text,
                            OnCompleteListener<Boolean> listener) {
        if (!available) return;
        try {
            Map<String, Object> comment = new HashMap<>();
            comment.put("userId", userId); comment.put("nickname", nickname);
            comment.put("text", text); comment.put("timestamp", System.currentTimeMillis());
            db.collection("posts").document(postId).update("comments", FieldValue.arrayUnion(comment))
                    .addOnSuccessListener(v -> { if (listener != null) listener.onResult(true); })
                    .addOnFailureListener(e -> { if (listener != null) listener.onResult(false); });
        } catch (Exception ignored) {}
    }

    // ═══════════ 用户帖子 ═══════════

    public void loadUserPosts(String userId, OnPostsLoadedListener listener) {
        if (!available) { if (listener != null) listener.onLoaded(null); return; }
        try {
            db.collection("posts").whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING).limit(30).get()
                    .addOnCompleteListener(task -> {
                        List<CommunityPost> result = new ArrayList<>();
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (DocumentSnapshot doc : task.getResult()) result.add(docToPost(doc));
                        }
                        if (listener != null) {
                            if (!task.isSuccessful()) listener.onLoaded(null);
                            else listener.onLoaded(result);
                        }
                    });
        } catch (Exception e) { if (listener != null) listener.onLoaded(null); }
    }

    // ═══════════ 举报 ═══════════

    public void reportPost(String postId, String userId, String reason) {
        if (!available) return;
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("postId", postId); report.put("reporterId", userId);
            report.put("reason", reason); report.put("timestamp", System.currentTimeMillis());
            db.collection("reports").add(report);
        } catch (Exception ignored) {}
    }

    // ═══════════ 关注系统 ═══════════

    public void followUser(String followerId, String followingId, String followerName) {
        if (!available) return;
        try {
            Map<String, Object> f = new HashMap<>();
            f.put("followerId", followerId); f.put("followingId", followingId);
            f.put("followerName", followerName); f.put("timestamp", System.currentTimeMillis());
            db.collection("follows").document(followerId + "_" + followingId).set(f);
        } catch (Exception ignored) {}
    }

    public void unfollowUser(String followerId, String followingId) {
        if (!available) return;
        try {
            db.collection("follows").document(followerId + "_" + followingId).delete();
        } catch (Exception ignored) {}
    }

    public void isFollowing(String followerId, String followingId,
                             java.util.function.Consumer<Boolean> cb) {
        if (!available) { cb.accept(false); return; }
        try {
            db.collection("follows").document(followerId + "_" + followingId).get()
                    .addOnCompleteListener(task -> cb.accept(task.isSuccessful()
                            && task.getResult() != null && task.getResult().exists()));
        } catch (Exception e) { cb.accept(false); }
    }

    public void getFollowerCount(String userId, java.util.function.Consumer<Integer> cb) {
        if (!available) { cb.accept(0); return; }
        try {
            db.collection("follows").whereEqualTo("followingId", userId).get()
                    .addOnCompleteListener(task -> {
                        int c = task.isSuccessful() && task.getResult() != null
                                ? task.getResult().size() : 0;
                        cb.accept(c);
                    });
        } catch (Exception e) { cb.accept(0); }
    }

    public void getFollowingCount(String userId, java.util.function.Consumer<Integer> cb) {
        if (!available) { cb.accept(0); return; }
        try {
            db.collection("follows").whereEqualTo("followerId", userId).get()
                    .addOnCompleteListener(task -> {
                        int c = task.isSuccessful() && task.getResult() != null
                                ? task.getResult().size() : 0;
                        cb.accept(c);
                    });
        } catch (Exception e) { cb.accept(0); }
    }

    // ═══════════ 挑战系统 ═══════════

    public void createChallenge(String creatorId, String creatorName, String title,
                                 String goalType, double goalValue, long endDate,
                                 OnCompleteListener<String> listener) {
        if (!available) { if (listener != null) listener.onResult(null); return; }
        try {
            Map<String, Object> ch = new HashMap<>();
            ch.put("creatorId", creatorId); ch.put("creatorName", creatorName);
            ch.put("title", title); ch.put("goalType", goalType); ch.put("goalValue", goalValue);
            ch.put("endDate", endDate); ch.put("createdAt", System.currentTimeMillis());
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> creator = new HashMap<>();
            creator.put("userId", creatorId); creator.put("nickname", creatorName);
            creator.put("progress", 0.0);
            parts.add(creator);
            ch.put("participants", parts);
            db.collection("challenges").add(ch)
                    .addOnSuccessListener(doc -> { if (listener != null) listener.onResult(doc.getId()); })
                    .addOnFailureListener(e -> { if (listener != null) listener.onResult(null); });
        } catch (Exception e) { if (listener != null) listener.onResult(null); }
    }

    public void loadChallenges(OnChallengesLoadedListener listener) {
        if (!available) { if (listener != null) listener.onLoaded(new ArrayList<>()); return; }
        try {
            db.collection("challenges")
                    .whereGreaterThan("endDate", System.currentTimeMillis())
                    .orderBy("endDate").limit(20).get()
                    .addOnCompleteListener(task -> {
                        List<Challenge> result = new ArrayList<>();
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (DocumentSnapshot doc : task.getResult())
                                result.add(docToChallenge(doc));
                        }
                        if (listener != null) listener.onLoaded(result);
                    });
        } catch (Exception e) { if (listener != null) listener.onLoaded(new ArrayList<>()); }
    }

    public void joinChallenge(String challengeId, String userId, String nickname) {
        if (!available) return;
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("userId", userId); p.put("nickname", nickname); p.put("progress", 0.0);
            db.collection("challenges").document(challengeId)
                    .update("participants", FieldValue.arrayUnion(p));
        } catch (Exception ignored) {}
    }

    public void updateChallengeProgress(String challengeId, String userId, double progress) {
        if (!available) return;
        try {
            db.collection("challenges").document(challengeId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc == null) return;
                        List<Map<String, Object>> parts =
                                (List<Map<String, Object>>) doc.get("participants");
                        if (parts == null) return;
                        for (int i = 0; i < parts.size(); i++) {
                            if (userId.equals(parts.get(i).get("userId"))) {
                                parts.get(i).put("progress", progress);
                                db.collection("challenges").document(challengeId)
                                        .update("participants", parts);
                                break;
                            }
                        }
                    });
        } catch (Exception ignored) {}
    }

    private Challenge docToChallenge(DocumentSnapshot doc) {
        Challenge c = new Challenge();
        c.id = doc.getId();
        c.title = doc.getString("title"); c.creatorName = doc.getString("creatorName");
        c.goalType = doc.getString("goalType");
        Double gv = doc.getDouble("goalValue"); c.goalValue = gv != null ? gv : 0;
        Long ed = doc.getLong("endDate"); c.endDate = ed != null ? ed : 0;
        c.participants = (List<Map<String, Object>>) doc.get("participants");
        if (c.participants == null) c.participants = new ArrayList<>();
        return c;
    }

    // ═══════════ 活动流 ═══════════

    public void publishActivity(String userId, String nickname, String title,
                                 String content, String activityType) {
        if (!available) return;
        try {
            Map<String, Object> post = new HashMap<>();
            post.put("userId", userId); post.put("nickname", nickname);
            post.put("title", title); post.put("content", content);
            post.put("imageUri", "");
            post.put("timestamp", System.currentTimeMillis());
            post.put("likes", new ArrayList<String>());
            post.put("comments", new ArrayList<Map<String, Object>>());
            post.put("activityType", activityType);
            db.collection("posts").add(post);
        } catch (Exception ignored) {}
    }

    // ═══════════ 工具 ═══════════

    private CommunityPost docToPost(DocumentSnapshot doc) {
        CommunityPost p = new CommunityPost();
        p.id = doc.getId();
        p.userId = doc.getString("userId"); p.nickname = doc.getString("nickname");
        p.title = doc.getString("title"); p.content = doc.getString("content");
        p.imageUri = doc.getString("imageUri");
        Long ts = doc.getLong("timestamp"); p.timestamp = ts != null ? ts : 0;
        List<String> likes = (List<String>) doc.get("likes");
        p.likeCount = likes != null ? likes.size() : 0;
        List<Map<String, Object>> comments = (List<Map<String, Object>>) doc.get("comments");
        p.commentCount = comments != null ? comments.size() : 0;
        p.comments = comments != null ? comments : new ArrayList<>();
        return p;
    }

    public static class CommunityPost {
        public String id, userId, nickname, title, content, imageUri;
        public long timestamp;
        public int likeCount, commentCount;
        public List<Map<String, Object>> comments;
    }

    public static class Challenge {
        public String id, title, creatorName, goalType;
        public double goalValue;
        public long endDate;
        public List<Map<String, Object>> participants;
    }

    public interface OnCompleteListener<T> { void onResult(T result); }
    public interface OnPostsLoadedListener { void onLoaded(List<CommunityPost> posts); }
    public interface OnPostLoadedListener { void onLoaded(CommunityPost post); }
    public interface OnChallengesLoadedListener { void onLoaded(List<Challenge> challenges); }
}
