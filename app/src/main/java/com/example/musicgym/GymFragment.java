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

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GymFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gym, container, false);

        view.findViewById(R.id.gym_card_running).setOnClickListener(v ->
                startWorkout("跑步"));

        view.findViewById(R.id.gym_card_cycling).setOnClickListener(v ->
                startWorkout("骑行"));

        view.findViewById(R.id.gym_card_walking).setOnClickListener(v ->
                startWorkout("步行"));

        view.findViewById(R.id.gym_card_strength).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), StrengthActivity.class)));

        // 动态卡片内容
        loadCardSubtitles(view);

        return view;
    }

    private void startWorkout(String sportType) {
        Intent intent = new Intent(requireContext(), WorkoutActivity.class);
        intent.putExtra("sport_type", sportType);
        startActivity(intent);
    }

    private void loadCardSubtitles(View view) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<WorkoutRecord> records = db.workoutRecordDao().getAllRecords();
            List<StrengthRecord> sRecs = db.strengthRecordDao().getAllRecords();

            requireActivity().runOnUiThread(() -> {
                if (records == null || records.isEmpty()) return;
                setLastWorkout(view, R.id.gym_card_running, records, "Running");
                setLastWorkout(view, R.id.gym_card_cycling, records, "Cycling");
                setLastWorkout(view, R.id.gym_card_walking, records, "Walking");
                if (sRecs != null && !sRecs.isEmpty()) {
                    StrengthRecord r = sRecs.get(0);
                    if (r.getDate() != null) {
                        String info = "上次 " + r.getDate().substring(5) +
                                " · " + (r.getDurationSeconds() / 60) + "min";
                        setCardSubtitle(view, R.id.gym_card_strength, info);
                    }
                }
            });
            exec.shutdown();
        });
    }

    private void setLastWorkout(View root, int cardId, List<WorkoutRecord> records, String sportType) {
        for (WorkoutRecord r : records) {
            if (sportType.equals(r.getSportType())) {
                String info = "上次 " + (r.getDate() != null ? r.getDate().substring(5) : "") +
                        " · " + String.format(Locale.getDefault(), "%.1fkm", r.getDistanceKm());
                setCardSubtitle(root, cardId, info);
                return;
            }
        }
    }

    private void setCardSubtitle(View root, int cardId, String text) {
        View card = root.findViewById(cardId);
        if (card instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) card;
            // 卡片内的LinearLayout → 第二个子View是内容区(第一个是图标,第二个是信息)
            if (vg.getChildCount() > 0 && vg.getChildAt(0) instanceof ViewGroup) {
                ViewGroup inner = (ViewGroup) vg.getChildAt(0);
                // 查找副标题: 信息区内的第二个TextView
                for (int i = 0; i < inner.getChildCount(); i++) {
                    View child = inner.getChildAt(i);
                    if (child instanceof ViewGroup) {
                        ViewGroup infoCol = (ViewGroup) child;
                        if (infoCol.getChildCount() >= 2) {
                            View sub = infoCol.getChildAt(1);
                            if (sub instanceof TextView) {
                                ((TextView) sub).setText(text);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
