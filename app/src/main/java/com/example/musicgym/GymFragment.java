package com.example.musicgym;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

/** GYM Tab — 运动类型选择（MVVM 架构） */
public class GymFragment extends Fragment {

    private GymViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gym, container, false);

        vm = new ViewModelProvider(this).get(GymViewModel.class);

        // 导航（保持不变 — View 层职责）
        view.findViewById(R.id.gym_card_running).setOnClickListener(v ->
                startWorkout("跑步"));
        view.findViewById(R.id.gym_card_cycling).setOnClickListener(v ->
                startWorkout("骑行"));
        view.findViewById(R.id.gym_card_walking).setOnClickListener(v ->
                startWorkout("步行"));
        view.findViewById(R.id.gym_card_strength).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), StrengthActivity.class)));

        // 观察上次训练信息
        vm.getRunInfo().observe(getViewLifecycleOwner(),
                t -> setCardSubtitle(view, R.id.gym_card_running, t));
        vm.getCycleInfo().observe(getViewLifecycleOwner(),
                t -> setCardSubtitle(view, R.id.gym_card_cycling, t));
        vm.getWalkInfo().observe(getViewLifecycleOwner(),
                t -> setCardSubtitle(view, R.id.gym_card_walking, t));
        vm.getStrengthInfo().observe(getViewLifecycleOwner(),
                t -> setCardSubtitle(view, R.id.gym_card_strength, t));

        vm.loadLastWorkouts();

        return view;
    }

    private void startWorkout(String sportType) {
        Intent intent = new Intent(requireContext(), WorkoutActivity.class);
        intent.putExtra("sport_type", sportType);
        startActivity(intent);
    }

    /** 设置卡片副标题（View 层逻辑，保留在 Fragment） */
    private void setCardSubtitle(View root, int cardId, String text) {
        if (text == null || text.isEmpty()) return;
        View card = root.findViewById(cardId);
        if (card instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) card;
            if (vg.getChildCount() > 0 && vg.getChildAt(0) instanceof ViewGroup) {
                ViewGroup inner = (ViewGroup) vg.getChildAt(0);
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
