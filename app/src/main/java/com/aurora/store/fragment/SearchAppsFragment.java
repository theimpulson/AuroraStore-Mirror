/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.aurora.store.fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.EndlessScrollListener;
import com.aurora.store.ErrorType;
import com.aurora.store.Filter;
import com.aurora.store.R;
import com.aurora.store.activity.AuroraActivity;
import com.aurora.store.adapter.EndlessAppsAdapter;
import com.aurora.store.model.App;
import com.aurora.store.sheet.FilterBottomSheet;
import com.aurora.store.task.SearchTask;
import com.aurora.store.utility.Log;
import com.aurora.store.utility.NetworkUtil;
import com.aurora.store.utility.Util;
import com.aurora.store.utility.ViewUtil;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SearchAppsFragment extends BaseFragment {

    @BindView(R.id.search_apps_list)
    RecyclerView recyclerView;
    @BindView(R.id.filter_fab)
    FloatingActionButton filterFab;
    @BindView(R.id.searchQuery)
    EditText searchQuery;
    @BindView(R.id.related_chip_group)
    ChipGroup relatedChipGroup;
    @BindView(R.id.view_progress)
    RelativeLayout layoutProgress;

    private Context context;
    private View view;
    private String query;
    private List<String> relatedTags = new ArrayList<>();
    private BottomNavigationView bottomNavigationView;
    private EndlessAppsAdapter endlessAppsAdapter;
    private SearchTask searchTask;

    private String getQuery() {
        return query;
    }

    private void setQuery(String query) {
        this.query = query;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        searchTask = new SearchTask(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_search_applist, container, false);
        ButterKnife.bind(this, view);
        Bundle arguments = getArguments();
        if (arguments != null) {
            setQuery(arguments.getString("SearchQuery"));
            searchQuery.setText(getQuery());
        } else
            Log.e("No category id provided");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupQueryEdit();
        setErrorView(ErrorType.UNKNOWN);
        setupRecycler();
        if (getActivity() instanceof AuroraActivity) {
            bottomNavigationView = ((AuroraActivity) getActivity()).getBottomNavigation();
            setBaseBottomNavigationView(bottomNavigationView);
        }
        if (bottomNavigationView != null)
            ViewUtil.hideBottomNav(bottomNavigationView, true);
        filterFab.show();
        filterFab.setOnClickListener(v -> getFilterDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (endlessAppsAdapter == null || endlessAppsAdapter.isDataEmpty())
            fetchSearchAppsList(false);
    }

    @Override
    public void onDestroy() {
        Glide.with(this).pauseAllRequests();
        disposable.dispose();
        if (bottomNavigationView != null)
            ViewUtil.showBottomNav(bottomNavigationView, true);
        if (Util.filterSearchNonPersistent(context))
            new Filter(context).resetFilterPreferences();
        super.onDestroy();
    }

    private void setupQueryEdit() {
        searchQuery.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                searchQuery.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_search, 0);
            else
                searchQuery.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_edit, 0);
        });
        searchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                query = searchQuery.getText().toString();
                fetchSearchAppsList(false);
                searchQuery.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_edit, 0);
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                searchQuery.clearFocus();
                return true;
            } else
                return false;
        });
    }

    private void getFilterDialog() {
        FilterBottomSheet filterSheet = new FilterBottomSheet();
        filterSheet.setCancelable(true);
        filterSheet.setOnApplyListener(v -> {
            filterSheet.dismiss();
            recyclerView.removeAllViewsInLayout();
            fetchSearchAppsList(false);
        });
        filterSheet.show(getChildFragmentManager(), "FILTER");
    }

    private void getIterator() {
        try {
            iterator = null;
            iterator = getIterator(getQuery());
            iterator.setEnableFilter(true);
            iterator.setFilter(new Filter(getContext()).getFilterPreferences());
            relatedTags = iterator.getRelatedTags();
        } catch (Exception e) {
            processException(e);
        }
    }

    private void fetchSearchAppsList(boolean shouldIterate) {
        if (!shouldIterate)
            getIterator();
        disposable.add(Observable.fromCallable(() -> searchTask.getSearchResults(iterator))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(d -> ViewUtil.showWithAnimation(layoutProgress))
                .doOnTerminate(() -> ViewUtil.hideWithAnimation(layoutProgress))
                .subscribe(appList -> {
                    if (view != null) {
                        if (shouldIterate) {
                            addApps(appList);
                        } else if (appList.isEmpty() && endlessAppsAdapter.isDataEmpty()) {
                            setErrorView(ErrorType.NO_SEARCH);
                            switchViews(true);
                        } else {
                            switchViews(false);
                            if (endlessAppsAdapter != null)
                                endlessAppsAdapter.addData(appList);
                            if (!relatedTags.isEmpty())
                                setupTags();
                        }
                    }
                }, err -> {
                    Log.e(err.getMessage());
                    processException(err);
                }));
    }

    private void addApps(List<App> appsToAdd) {
        if (!appsToAdd.isEmpty()) {
            for (App app : appsToAdd)
                endlessAppsAdapter.add(app);
            endlessAppsAdapter.notifyItemInserted(endlessAppsAdapter.getItemCount() - 1);
        }

        /*
         * Search results are scarce if filter are too strict, in this case endless scroll events
         * fail to fetch next batch of apps, so manually fetch at least 10 apps until available.
         */
        disposable.add(Observable.interval(1000, 2000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    if (iterator.hasNext() && endlessAppsAdapter.getItemCount() < 10) {
                        iterator.next();
                    }
                }, e -> Log.e(e.getMessage())));
    }

    private void setupRecycler() {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        endlessAppsAdapter = new EndlessAppsAdapter(context);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this.getActivity(), R.anim.anim_falldown));
        recyclerView.setAdapter(endlessAppsAdapter);
        EndlessScrollListener endlessScrollListener = new EndlessScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                fetchSearchAppsList(true);
            }
        };
        recyclerView.addOnScrollListener(endlessScrollListener);
        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (velocityY < 0) {
                    filterFab.show();
                } else if (velocityY > 0) {
                    filterFab.hide();
                }
                return false;
            }
        });
    }

    private void setupTags() {
        relatedChipGroup.removeAllViews();
        int i = 0;
        for (String tag : relatedTags) {
            final int color = ViewUtil.getSolidColors(i++);
            Chip chip = new Chip(context);
            chip.setText(tag);
            chip.setChipStrokeWidth(3);
            chip.setChipStrokeColor(ColorStateList.valueOf(color));
            chip.setChipBackgroundColor(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 100)));
            chip.setRippleColor(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 200)));
            chip.setCheckedIcon(context.getDrawable(R.drawable.ic_chip_checked));
            chip.setOnClickListener(v -> {
                if (chip.isChecked()) {
                    query = query + " " + tag;
                    fetchData();
                    searchQuery.setText(query);
                } else {
                    query = query.replace(tag, "");
                    fetchData();
                    searchQuery.setText(query);
                }
            });
            relatedChipGroup.addView(chip);
        }
    }

    @Override
    protected void fetchData() {
        fetchSearchAppsList(false);
    }

    @Override
    protected View.OnClickListener errRetry() {
        return v -> {
            if (NetworkUtil.isConnected(context)) {
                fetchData();
            } else {
                setErrorView(ErrorType.NO_NETWORK);
            }
            ((Button) v).setText(getString(R.string.action_retry_ing));
            ((Button) v).setEnabled(false);
        };
    }
}