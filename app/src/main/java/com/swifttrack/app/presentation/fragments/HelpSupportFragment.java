package com.swifttrack.app.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.swifttrack.app.data.repository.TicketRepository;
import com.swifttrack.app.databinding.FragmentHelpSupportBinding;

public class HelpSupportFragment extends Fragment {

    private FragmentHelpSupportBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHelpSupportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnRecoverTicket.setOnClickListener(v -> {
            String ref = binding.etRecoveryRef.getText() != null ? binding.etRecoveryRef.getText().toString().trim() : "";
            if (ref.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a booking reference", Toast.LENGTH_SHORT).show();
                return;
            }

            // Perform server sync & database lookup before suggesting new purchase
            TicketRepository repo = new TicketRepository(requireContext());
            repo.refreshTickets();

            Toast.makeText(requireContext(), "Synchronizing with server for reference: " + ref, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
