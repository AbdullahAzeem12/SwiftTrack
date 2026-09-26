package com.swifttrack.app.presentation.activities;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.swifttrack.app.R;
import com.swifttrack.app.databinding.ActivityMainBinding;
import com.swifttrack.app.presentation.adapters.MainTabsPagerAdapter;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge transparent system navigation bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup ViewPager2 for horizontal 1:1 swipe-to-navigate tab transitions
        MainTabsPagerAdapter pagerAdapter = new MainTabsPagerAdapter(this);
        binding.viewPagerTabs.setAdapter(pagerAdapter);
        binding.viewPagerTabs.setOffscreenPageLimit(4);

        // Custom page transformer for smooth luxury tab sliding
        binding.viewPagerTabs.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1.0f - (absPos * 0.15f));
        });

        binding.bottomNavigation.setupWithViewPager2(binding.viewPagerTabs);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            binding.bottomNavigation.setupWithNavController(navController);

            // Initial badge demonstration for service alerts
            binding.bottomNavigation.setBadge(R.id.alertsFragment, 2);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.homeBookFragment || id == R.id.ticketsListFragment ||
                    id == R.id.timetableFragment || id == R.id.alertsFragment ||
                    id == R.id.accountFragment) {
                    // Top-Level Main Tabs: Show Bottom Navigation Dock & ViewPager2
                    binding.bottomNavigation.setVisibility(View.VISIBLE);
                    binding.viewPagerTabs.setVisibility(View.VISIBLE);
                    binding.navHostFragment.setVisibility(View.GONE);
                } else {
                    // All Nested Sub-pages (CardDetails, Payment, Checkout, FareSelection, Tickets, Auth): Hide Bottom Nav Dock
                    binding.bottomNavigation.setVisibility(View.GONE);
                    binding.viewPagerTabs.setVisibility(View.GONE);
                    binding.navHostFragment.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    public void updateAlertsBadge(int count) {
        if (binding != null && binding.bottomNavigation != null) {
            binding.bottomNavigation.setBadge(R.id.alertsFragment, count);
        }
    }
}
