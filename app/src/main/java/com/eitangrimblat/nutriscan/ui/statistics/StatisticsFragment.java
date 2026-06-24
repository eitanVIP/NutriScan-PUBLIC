package com.eitangrimblat.nutriscan.ui.statistics;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eitangrimblat.nutriscan.R;
import com.eitangrimblat.nutriscan.firebase.Auth;
import com.eitangrimblat.nutriscan.ui.activities.MainActivity;
import com.google.android.material.snackbar.Snackbar;

/**
 * Fragment that displays nutritional statistics and insights generated from the user's scanned products.
 */
public class StatisticsFragment extends Fragment {
    private StatisticsViewModel viewModel;
    private View fragmentView;

    private SwipeRefreshLayout srlStats;

    private TextView tvScoreValue, tvScoreStatus;
    private ProgressBar pbScore;

    private TextView tvNutrient1Value, tvNutrient2Value, tvNutrient3Value;

    private TextView tvInsight1Title, tvInsight1Content;
    private TextView tvInsight2Title, tvInsight2Content;
    private TextView tvInsight3Title, tvInsight3Content;

    private TextView tvSpecialScoreTitle, tvSpecialScoreValue, tvSpecialScoreStatus;
    private ProgressBar pbSpecialScore;

    /**
     * Initializes the StatisticsViewModel and redirects to MainActivity if the user is not authenticated.
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        if (Auth.getCurrentUser() == null) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    /**
     * Inflates the fragment layout and triggers UI and observer setup.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_statstics, container, false);

        initViews();
        setupObservers();

        return fragmentView;
    }

    /**
     * Initializes the UI views from the inflated layout.
     */
    private void initViews() {
        srlStats = fragmentView.findViewById(R.id.srlStats);
        srlStats.setEnabled(false);

        tvScoreValue = fragmentView.findViewById(R.id.tvScoreValue);
        tvScoreStatus = fragmentView.findViewById(R.id.tvScoreStatus);
        pbScore = fragmentView.findViewById(R.id.pbScore);

        tvNutrient1Value = fragmentView.findViewById(R.id.tvNutrient1Value);
        tvNutrient2Value = fragmentView.findViewById(R.id.tvNutrient2Value);
        tvNutrient3Value = fragmentView.findViewById(R.id.tvNutrient3Value);

        tvInsight1Title = fragmentView.findViewById(R.id.tvInsight1Title);
        tvInsight1Content = fragmentView.findViewById(R.id.tvInsight1Content);
        tvInsight2Title = fragmentView.findViewById(R.id.tvInsight2Title);
        tvInsight2Content = fragmentView.findViewById(R.id.tvInsight2Content);
        tvInsight3Title = fragmentView.findViewById(R.id.tvInsight3Title);
        tvInsight3Content = fragmentView.findViewById(R.id.tvInsight3Content);

        tvSpecialScoreTitle = fragmentView.findViewById(R.id.tvSpecialScoreTitle);
        tvSpecialScoreValue = fragmentView.findViewById(R.id.tvSpecialScoreValue);
        tvSpecialScoreStatus = fragmentView.findViewById(R.id.tvSpecialScoreStatus);
        pbSpecialScore = fragmentView.findViewById(R.id.pbSpecialScore);
    }

    /**
     * Sets up observers for the ViewModel's loading state, statistics data, and UI events.
     */
    private void setupObservers() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            srlStats.setRefreshing(loading);
        });

        viewModel.getStatsData().observe(getViewLifecycleOwner(), data -> {
            tvScoreValue.setText(data.score + "/100");
            tvScoreValue.setTextColor(evaluateScoreColor(data.score));
            pbScore.setProgress(data.score);
            pbScore.setProgressTintList(ColorStateList.valueOf(evaluateScoreColor(data.score)));
            tvScoreStatus.setText(evaluateScore(data.score));
            tvScoreStatus.setTextColor(evaluateScoreColor(data.score));

            tvNutrient1Value.setText(data.nutrient1Value);
            tvNutrient2Value.setText(data.nutrient2Value);
            tvNutrient3Value.setText(data.nutrient3Value);

            tvInsight1Title.setText(data.insight1Title);
            tvInsight1Content.setText(data.insight1Content);
            tvInsight2Title.setText(data.insight2Title);
            tvInsight2Content.setText(data.insight2Content);
            tvInsight3Title.setText(data.insight3Title);
            tvInsight3Content.setText(data.insight3Content);

            tvSpecialScoreTitle.setText(data.specialScoreTitle);
            tvSpecialScoreValue.setText(data.specialScore + "/100");
            tvSpecialScoreValue.setTextColor(evaluateScoreColor(data.specialScore));
            pbSpecialScore.setProgress(data.specialScore);
            pbSpecialScore.setProgressTintList(ColorStateList.valueOf(evaluateScoreColor(data.specialScore)));
            tvSpecialScoreStatus.setText(evaluateScore(data.specialScore));
            tvSpecialScoreStatus.setTextColor(evaluateScoreColor(data.specialScore));
        });

        viewModel.getUiEvent().observe(getViewLifecycleOwner(), event -> {
            Snackbar.make(fragmentView, event, Snackbar.LENGTH_LONG).show();
        });
    }

    /**
     * Evaluates the score and returns a descriptive string.
     * @param score The score to evaluate (0-100).
     * @return A status string (e.g., "Excellent", "Good").
     */
    private String evaluateScore(int score) {
        if (score >= 80) {
            return "Excellent";
        } else if (score >= 60) {
            return "Good";
        } else if (score >= 40) {
            return "Fair";
        } else {
            return "Needs Improvement";
        }
    }

    /**
     * Evaluates the score and returns a corresponding color.
     * @param score The score to evaluate (0-100).
     * @return An ARGB color integer.
     */
    private int evaluateScoreColor(int score) {
        if (score >= 80) {
            return 0xFF4CAF50; // Green
        } else if (score >= 60) {
            return 0xFF8BC34A; // Light Green
        } else if (score >= 40) {
            return 0xFFFFC107; // Yellow
        } else {
            return 0xFFF44336; // Red
        }
    }
}