package com.example.musicgym;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class GymFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gym, container, false);

        view.findViewById(R.id.gym_card_running).setOnClickListener(v ->
                startWorkout("跑步"));

        view.findViewById(R.id.gym_card_cycling).setOnClickListener(v ->
                startWorkout("骑行"));

        view.findViewById(R.id.gym_card_walking).setOnClickListener(v ->
                startWorkout("步行"));

        view.findViewById(R.id.gym_card_strength).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), StrengthActivity.class)));

        return view;
    }

    private void startWorkout(String sportType) {
        Intent intent = new Intent(requireContext(), WorkoutActivity.class);
        intent.putExtra("sport_type", sportType);
        startActivity(intent);
    }
}
