package com.swifttrack.app.presentation.dialogs;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.swifttrack.app.R;
import com.swifttrack.app.databinding.BottomSheetFaqsBinding;

import java.util.ArrayList;
import java.util.List;

public class FaqsBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetFaqsBinding binding;

    public static class FaqItem {
        public String id;
        public String question;
        public String answer;
        public String category;

        public FaqItem(String id, String question, String answer, String category) {
            this.id = id;
            this.question = question;
            this.answer = answer;
            this.category = category;
        }
    }

    private final List<FaqItem> allFaqs = new ArrayList<>();
    private String selectedCategory = "ALL";
    private String searchQuery = "";

    public static FaqsBottomSheetDialogFragment newInstance() {
        return new FaqsBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetFaqsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnCloseFaqs.setOnClickListener(v -> dismiss());
        binding.btnFaqContactSupport.setOnClickListener(v -> {
            dismiss();
            ContactSupportBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "contact_support");
        });

        setupCategoryChips();
        setupSearchInput();
        loadFaqsFromFirestore();
    }

    private void setupCategoryChips() {
        binding.chipGroupFaqCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_cat_booking)) {
                selectedCategory = "BOOKING";
            } else if (checkedIds.contains(R.id.chip_cat_live)) {
                selectedCategory = "LIVE";
            } else if (checkedIds.contains(R.id.chip_cat_account)) {
                selectedCategory = "ACCOUNT";
            } else if (checkedIds.contains(R.id.chip_cat_rewards)) {
                selectedCategory = "REWARDS";
            } else {
                selectedCategory = "ALL";
            }
            renderFilteredFaqs();
        });
    }

    private void setupSearchInput() {
        binding.etFaqSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                renderFilteredFaqs();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadFaqsFromFirestore() {
        binding.progressFaqs.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("appContent").document("faqs")
                .collection("items")
                .get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    binding.progressFaqs.setVisibility(View.GONE);
                    allFaqs.clear();

                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String q = doc.getString("question");
                            String a = doc.getString("answer");
                            String c = doc.getString("category");
                            if (q != null && a != null) {
                                allFaqs.add(new FaqItem(doc.getId(), q, a, c != null ? c : "GENERAL"));
                            }
                        }
                    } else {
                        // Seed built-in comprehensive help articles
                        populateBuiltInFaqs();
                    }
                    renderFilteredFaqs();
                });
    }

    private void populateBuiltInFaqs() {
        allFaqs.add(new FaqItem("f1", "How do I book a Heathrow Express ticket?", "Select your origin (Paddington or Heathrow Terminal), travel date, passengers, and fare class on the Book tab. Confirm payment to receive instant digital QR tickets.", "BOOKING"));
        allFaqs.add(new FaqItem("f2", "Can I use biometric fingerprint login?", "Yes! Toggle 'Biometric Re-entry' under Account & Security. SwiftTrack registers an encrypted key in Android Keystore for instant identity verification.", "ACCOUNT"));
        allFaqs.add(new FaqItem("f3", "Are live train departures accurate?", "All live departure boards and disruption notices are fetched in real-time from official National Rail / TransportAPI feeds.", "LIVE"));
        allFaqs.add(new FaqItem("f4", "How do SwiftTrack Reward Points work?", "Earn 10 points for every £1 spent on bookings. Points can be redeemed for fare discounts using promo codes like SWIFTTRACK20.", "REWARDS"));
        allFaqs.add(new FaqItem("f5", "What is the journey time between Paddington and Heathrow?", "Non-stop Heathrow Express trains take 15 minutes to Terminals 2 & 3, and 6 additional minutes to Terminal 5.", "BOOKING"));
    }

    private void renderFilteredFaqs() {
        if (binding == null) return;
        binding.layoutFaqListContainer.removeAllViews();

        List<FaqItem> filtered = new ArrayList<>();
        for (FaqItem item : allFaqs) {
            boolean matchesCategory = "ALL".equalsIgnoreCase(selectedCategory) ||
                    item.category.equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = searchQuery.isEmpty() ||
                    item.question.toLowerCase().contains(searchQuery) ||
                    item.answer.toLowerCase().contains(searchQuery);

            if (matchesCategory && matchesSearch) {
                filtered.add(item);
            }
        }

        if (filtered.isEmpty()) {
            TextView emptyTv = new TextView(requireContext());
            emptyTv.setText("No help articles found matching your criteria.");
            emptyTv.setTextColor(getResources().getColor(R.color.text_secondary, null));
            emptyTv.setTextSize(13);
            emptyTv.setPadding(16, 32, 16, 32);
            binding.layoutFaqListContainer.addView(emptyTv);
            return;
        }

        for (FaqItem faq : filtered) {
            View card = getLayoutInflater().inflate(R.layout.item_faq_expandable, binding.layoutFaqListContainer, false);
            TextView tvQuestion = card.findViewById(R.id.tv_faq_question);
            TextView tvAnswer = card.findViewById(R.id.tv_faq_answer);
            ImageView ivChevron = card.findViewById(R.id.iv_faq_chevron);
            View header = card.findViewById(R.id.layout_faq_header);

            tvQuestion.setText(faq.question);
            tvAnswer.setText(faq.answer);

            header.setOnClickListener(v -> {
                boolean isExpanded = tvAnswer.getVisibility() == View.VISIBLE;
                if (isExpanded) {
                    tvAnswer.setVisibility(View.GONE);
                    ivChevron.animate().rotation(0f).setDuration(180).start();
                } else {
                    tvAnswer.setVisibility(View.VISIBLE);
                    ivChevron.animate().rotation(180f).setDuration(180).start();
                }
            });

            binding.layoutFaqListContainer.addView(card);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
