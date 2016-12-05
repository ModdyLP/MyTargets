/*
 * Copyright (C) 2016 Florian Dreier
 *
 * This file is part of MyTargets.
 *
 * MyTargets is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * MyTargets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package de.dreier.mytargets.fragments;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import de.dreier.mytargets.R;
import de.dreier.mytargets.activities.InputActivity;
import de.dreier.mytargets.activities.SimpleFragmentActivityBase;
import de.dreier.mytargets.databinding.FragmentEditRoundBinding;
import de.dreier.mytargets.managers.SettingsManager;
import de.dreier.mytargets.managers.dao.RoundDataSource;
import de.dreier.mytargets.managers.dao.RoundTemplateDataSource;
import de.dreier.mytargets.managers.dao.StandardRoundDataSource;
import de.dreier.mytargets.managers.dao.TrainingDataSource;
import de.dreier.mytargets.shared.models.Round;
import de.dreier.mytargets.shared.models.RoundTemplate;
import de.dreier.mytargets.shared.models.StandardRound;
import de.dreier.mytargets.shared.models.Training;
import de.dreier.mytargets.shared.utils.StandardRoundFactory;
import de.dreier.mytargets.utils.IntentWrapper;
import de.dreier.mytargets.utils.ToolbarUtils;
import de.dreier.mytargets.utils.transitions.FabTransformUtil;

import static de.dreier.mytargets.fragments.EditableListFragmentBase.ITEM_ID;

public class EditRoundFragment extends EditFragmentBase {
    private static final String ROUND_ID = "round_id";
    private long trainingId = -1;
    private long roundId = -1;
    private FragmentEditRoundBinding binding;

    @NonNull
    protected static IntentWrapper createIntent(Training training) {
        return new IntentWrapper(SimpleFragmentActivityBase.EditRoundActivity.class)
                .with(ITEM_ID, training.getId());
    }

    @NonNull
    protected static IntentWrapper editIntent(Training training, Round round) {
        return new IntentWrapper(SimpleFragmentActivityBase.EditRoundActivity.class)
                .with(ITEM_ID, training.getId())
                .with(ROUND_ID, round.getId());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil
                .inflate(inflater, R.layout.fragment_edit_round, container, false);

        Bundle arguments = getArguments();
        if (arguments != null) {
            trainingId = arguments.getLong(ITEM_ID, -1);
            roundId = arguments.getLong(ROUND_ID, -1);
        }

        ToolbarUtils.setSupportActionBar(this, binding.toolbar);
        ToolbarUtils.showUpAsX(this);
        setHasOptionsMenu(true);

        binding.arrows.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                updateArrowsLabel();
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
            }
        });
        binding.target.setOnActivityResultContext(this);
        binding.distance.setOnActivityResultContext(this);

        if (roundId == -1) {
            ToolbarUtils.setTitle(this, R.string.new_round);
            loadRoundDefaultValues();
            binding.comment.setText("");
            binding.removeButton.setVisibility(View.GONE);
        } else {
            ToolbarUtils.setTitle(this, R.string.edit_round);
            RoundDataSource roundDataSource = new RoundDataSource();
            Round round = roundDataSource.get(roundId);
            binding.distance.setItem(round.info.distance);
            binding.comment.setText(round.comment);
            binding.notEditable.setVisibility(View.GONE);
            StandardRoundDataSource standardRoundDataSource = new StandardRoundDataSource();
            StandardRound standardRound = standardRoundDataSource.get(round.info.standardRound);
            if (standardRound.club != StandardRoundFactory.CUSTOM_PRACTICE) {
                binding.distanceLayout.setVisibility(View.GONE);
            } else if (standardRound.rounds.size() > 1) {
                binding.removeButton.setOnClickListener(v -> {
                    roundDataSource.delete(round);
                    finish();
                });
            } else {
                binding.removeButton.setVisibility(View.GONE);
            }
        }
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FabTransformUtil.setup(getActivity(), binding.getRoot());
    }

    @Override
    protected void onSave() {
        finish();
        if (roundId == -1) {
            Round round = onSaveRound();
            RoundFragment.getIntent(round)
                    .withContext(this)
                    .noAnimation()
                    .start();
            InputActivity.createIntent(round)
                    .withContext(this)
                    .start();
        } else {
            onSaveRound();
            getActivity().overridePendingTransition(R.anim.left_in, R.anim.right_out);
        }
    }

    private Round onSaveRound() {
        RoundDataSource roundDataSource = new RoundDataSource();
        Training training = new TrainingDataSource().get(trainingId);
        StandardRoundDataSource standardRoundDataSource = new StandardRoundDataSource();
        StandardRound standardRound = standardRoundDataSource.get(training.standardRoundId);

        Round round;
        if (roundId != -1) {
            round = roundDataSource.get(roundId);
        } else {
            round = new Round();
            round.trainingId = trainingId;
            round.info = getRoundTemplate();
            round.info.standardRound = standardRound.getId();
        }

        round.comment = binding.comment.getText().toString();

        if (standardRound.club == StandardRoundFactory.CUSTOM_PRACTICE) {
            round.info.distance = binding.distance.getSelectedItem();
            round.info.index = standardRound.rounds.size();
            new RoundTemplateDataSource().update(round.info);
        }
        roundDataSource.update(round);

        return round;
    }

    private void updateArrowsLabel() {
        binding.arrowsLabel.setText(getResources()
                .getQuantityString(R.plurals.arrow, binding.arrows.getProgress(),
                        binding.arrows.getProgress()));
    }

    private void loadRoundDefaultValues() {
        binding.distance.setItem(SettingsManager.getDistance());
        binding.arrows.setProgress(SettingsManager.getArrowsPerPasse());
        binding.target.setItem(SettingsManager.getTarget());
    }

    @NonNull
    private RoundTemplate getRoundTemplate() {
        RoundTemplate roundTemplate = new RoundTemplate();
        roundTemplate.target = binding.target.getSelectedItem();
        roundTemplate.targetTemplate = roundTemplate.target;
        roundTemplate.arrowsPerEnd = binding.arrows.getProgress();
        roundTemplate.endCount = 1;
        roundTemplate.distance = binding.distance.getSelectedItem();

        SettingsManager.setTarget(roundTemplate.target);
        SettingsManager.setDistance(roundTemplate.distance);
        SettingsManager.setArrowsPerEnd(roundTemplate.arrowsPerEnd);
        return roundTemplate;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        binding.target.onActivityResult(requestCode, resultCode, data);
        binding.distance.onActivityResult(requestCode, resultCode, data);
    }
}