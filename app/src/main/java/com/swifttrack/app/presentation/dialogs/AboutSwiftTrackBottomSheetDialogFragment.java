package com.swifttrack.app.presentation.dialogs;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.swifttrack.app.databinding.BottomSheetAboutSwifttrackBinding;

public class AboutSwiftTrackBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetAboutSwifttrackBinding binding;

    public static AboutSwiftTrackBottomSheetDialogFragment newInstance() {
        return new AboutSwiftTrackBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAboutSwifttrackBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCloseAbout.setOnClickListener(v -> dismiss());

        loadRealVersionInfo();
        loadReleaseNotesFromFirestore();
        setupFooterButtons();
    }

    private void loadRealVersionInfo() {
        try {
            String versionName = requireActivity().getPackageManager()
                    .getPackageInfo(requireActivity().getPackageName(), 0).versionName;
            long versionCode = requireActivity().getPackageManager()
                    .getPackageInfo(requireActivity().getPackageName(), 0).getLongVersionCode();
            binding.tvAboutVersionTitle.setText("Version " + versionName + " (Build " + versionCode + ")");
        } catch (Exception e) {
            binding.tvAboutVersionTitle.setText("Version 1.0 (Build 34)");
        }
    }

    private void loadReleaseNotesFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("appContent")
                .document("releases")
                .get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;
                    if (doc != null && doc.exists() && doc.getString("notes") != null) {
                        binding.tvAboutReleaseNotes.setText(doc.getString("notes"));
                    }
                });
    }

    private void setupFooterButtons() {
        binding.btnAboutTerms.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("📜 Terms & Privacy Policy")
                    .setMessage("SwiftTrack is committed to protecting your security. All biometric templates remain inside the secure hardware Keymaster of your Android device. Payments are tokenized securely through PCI-DSS compliant channels.")
                    .setPositiveButton("Close", null)
                    .show();
        });

        binding.btnAboutLicenses.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("🔓 Open Source Licenses")
                    .setMessage("SwiftTrack incorporates open source libraries including AndroidX, Material Components, Firebase SDK, Gson, Retrofit, Room, and Glide. Licensed under Apache 2.0 and MIT Licenses.")
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
