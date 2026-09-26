package com.swifttrack.app.presentation.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.swifttrack.app.presentation.fragments.AccountFragment;
import com.swifttrack.app.presentation.fragments.AlertsFragment;
import com.swifttrack.app.presentation.fragments.HomeBookFragment;
import com.swifttrack.app.presentation.fragments.TicketsListFragment;
import com.swifttrack.app.presentation.fragments.TimetableFragment;

/**
 * Adapter for smooth horizontal swipe-to-navigate tab pages
 */
public class MainTabsPagerAdapter extends FragmentStateAdapter {

    public MainTabsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HomeBookFragment();
            case 1:
                return new TicketsListFragment();
            case 2:
                return new TimetableFragment();
            case 3:
                return new AlertsFragment();
            case 4:
                return new AccountFragment();
            default:
                return new HomeBookFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
