package com.example.musicgym;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** DeepSeek AI 训练计划生成器 */
public class AiPlanGenerator {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String apiKey;

    public interface PlanCallback {
        void onResult(String planJson, String error);
    }

    private static long lastCallTime;
    private static final long MIN_INTERVAL = 30000; // 30秒冷却(避免误触重复烧钱)

    public AiPlanGenerator(String apiKey) {
        this.apiKey = apiKey;
    }

    /** 检查是否允许调用（防止重复烧钱） */
    public static boolean canCall() {
        return System.currentTimeMillis() - lastCallTime > MIN_INTERVAL;
    }

    /** 生成 4 周训练计划 */
    public void generate(Context ctx, String goal, int daysPerWeek, PlanCallback cb) {
        executor.execute(() -> {
            if (apiKey.isEmpty()) {
                cb.onResult(null, "API Key 未配置");
                return;
            }
            if (!canCall()) {
                cb.onResult(null, "请等待30秒后再试(防止重复调用消耗额度)");
                return;
            }
            lastCallTime = System.currentTimeMillis();

            String prompt = buildPrompt(ctx, goal, daysPerWeek);
            Exception lastError = null;
            // 最多重试 3 次（网络波动）
            for (int attempt = 0; attempt < 3; attempt++) {
                if (attempt > 0) {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(60000);

                    JSONObject body = new JSONObject();
                    body.put("model", "deepseek-v4-pro");
                    body.put("temperature", 0.7);
                    body.put("max_tokens", 3000);

                    JSONArray messages = new JSONArray();
                    JSONObject sys = new JSONObject();
                    sys.put("role", "system");
                    sys.put("content", "你是一位NSCA认证力量教练。只输出JSON格式，不要任何解释文字。");
                    messages.put(sys);

                    JSONObject user = new JSONObject();
                    user.put("role", "user");
                    user.put("content", prompt);
                    messages.put(user);

                    body.put("messages", messages);

                    OutputStream os = conn.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();

                        JSONObject resp = new JSONObject(sb.toString());
                        String content = resp.getJSONArray("choices")
                                .getJSONObject(0).getJSONObject("message")
                                .getString("content");
                        cb.onResult(content, null);
                        conn.disconnect();
                        return;
                    } else if (code >= 500) {
                        // 服务端错误才重试
                        lastError = new Exception("API Error " + code);
                        conn.disconnect();
                        continue;
                    } else {
                        String errMsg = "API Error " + code;
                        if (conn.getErrorStream() != null) {
                            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(conn.getErrorStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) sb.append(line);
                            br.close();
                            errMsg += ": " + sb.toString();
                        }
                        cb.onResult(null, errMsg);
                        conn.disconnect();
                        return;
                    }
                } catch (Exception e) {
                    lastError = e;
                }
            }
            cb.onResult(null, lastError != null ? lastError.getMessage() : "多次重试失败");
        });
    }

    private String buildPrompt(Context ctx, String goal, int daysPerWeek) {
        StringBuilder sb = new StringBuilder();
        sb.append("基于以下数据生成一个").append(daysPerWeek).append("天/周的4周周期化训练计划。\n");
        sb.append("目标: ").append(goal).append("\n\n");

        // 读取训练历史
        try {
            AppDatabase db = AppDatabase.getInstance(ctx);
            List<StrengthRecord> recs = db.strengthRecordDao().getAllRecords();
            sb.append("用户最近训练数据:\n");
            int count = 0;
            for (StrengthRecord r : recs) {
                if (count++ >= 20) break;
                sb.append("日期: ").append(r.getDate())
                        .append(", 时长: ").append(r.getDurationSeconds() / 60).append("分钟\n");
                try {
                    JSONArray arr = new JSONArray(r.getExercisesJson());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject ex = arr.getJSONObject(i);
                        JSONArray sets = ex.getJSONArray("sets");
                        JSONObject last = sets.length() > 0
                                ? sets.getJSONObject(0) : null;
                        sb.append("  ").append(ex.optString("name"))
                                .append(": ").append(sets.length()).append("组");
                        if (last != null) sb.append(", ")
                                .append(last.optDouble("weight")).append("kg×")
                                .append(last.optInt("reps"));
                        sb.append("\n");
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        sb.append("\n输出JSON格式:\n");
        sb.append("{\n");
        sb.append("  \"rationale\": \"计划逻辑说明\",\n");
        sb.append("  \"weeks\": [{\n");
        sb.append("    \"week\": 1, \"focus\": \"适应性/容量建立\",\n");
        sb.append("    \"days\": [{\n");
        sb.append("      \"day\": 1, \"focus\": \"胸部+三头\",\n");
        sb.append("      \"exercises\": [\n");
        sb.append("        {\"name\": \"杠铃平板卧推\", \"sets\": 4, \"reps\": \"8-10\",\n");
        sb.append("         \"weight\": \"80% 1RM\", \"rpe\": \"7-8\",\n");
        sb.append("         \"rest\": \"90s\", \"note\": \"控制离心3秒\"}\n");
        sb.append("      ]\n");
        sb.append("    }]\n");
        sb.append("  }]\n");
        sb.append("}\n");
        sb.append("动作从以下选择: 杠铃平板卧推,上斜哑铃卧推,哑铃飞鸟,绳索夹胸,双杠臂屈伸,引体向上,高位下拉,杠铃划船,坐姿划船,传统硬拉,罗马尼亚硬拉,杠铃深蹲,腿举,腿弯举,臀推,保加利亚分腿蹲,杠铃推举,侧平举,面拉,杠铃弯举,哑铃弯举,三头绳索下压,卷腹,平板支撑 等");

        return sb.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        executor.shutdown();
    }
}
