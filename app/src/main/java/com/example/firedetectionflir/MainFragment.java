package com.example.firedetectionflir;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.firedetectionflir.databinding.FragmentMainBinding;
import com.flir.thermalsdk.live.Identity;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {
    private DiscoveryViewModel discoveryViewModel;
    private FragmentMainBinding fragmentMainBinding;
    private ArrayList<Identity> listIdentities = new ArrayList<Identity>();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    //private FragmentMainBinding fragmentMainBinding;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MainFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        discoveryViewModel = new ViewModelProvider(this).get(DiscoveryViewModel.class);

        fragmentMainBinding = FragmentMainBinding.inflate(inflater,container, false);

        fragmentMainBinding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoveryViewModel.startDiscovery();
            }
        });

        ListItemDeviceAdapter adapter = new ListItemDeviceAdapter(requireActivity(), listIdentities);

        fragmentMainBinding.devicesList.setAdapter(adapter);
        fragmentMainBinding.devicesList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        fragmentMainBinding.devicesList.addItemDecoration(
                new DividerItemDecoration(
                        getActivity(),
                        DividerItemDecoration.VERTICAL)
        );

        discoveryViewModel.foundIdentitiesLiveData.observe(getViewLifecycleOwner(), new Observer<ArrayList<Identity>>() {
            @Override
            public void onChanged(ArrayList<Identity> identities) {
                listIdentities.clear();
                listIdentities.addAll(identities);
                adapter.notifyDataSetChanged();
            }
        });

        discoveryViewModel.statusInfoLiveData.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                fragmentMainBinding.discoveryStatus.setText(s);
            }
        });

        return fragmentMainBinding.getRoot();
        // return inflater.inflate(R.layout.fragment_main, container, false);
    }
}