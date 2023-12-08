package com.example.inventorymanager.gettingstarted;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.inventorymanager.R;
import com.example.inventorymanager.databinding.FragmentRegisterSuccessBinding;
import com.google.android.material.transition.MaterialSharedAxis;

/**
 * Fragment for Register Success
 * Allows user to proceed to login
 */
public class RegisterSuccessFragment extends Fragment {

    private FragmentRegisterSuccessBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MaterialSharedAxis sharedAxis = new MaterialSharedAxis(MaterialSharedAxis.X, true);
        MaterialSharedAxis sharedAxis2 = new MaterialSharedAxis(MaterialSharedAxis.X, false);

        setEnterTransition(sharedAxis);
        setReenterTransition(sharedAxis);
        setReturnTransition(sharedAxis2);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterSuccessBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.proceedLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(RegisterSuccessFragment.this)
                        .navigate(R.id.action_registerSuccessFragment_to_loginFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}