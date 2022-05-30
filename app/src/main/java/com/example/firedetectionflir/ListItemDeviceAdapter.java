package com.example.firedetectionflir;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.flir.thermalsdk.live.Identity;

import java.util.ArrayList;
import java.util.List;

public class ListItemDeviceAdapter extends RecyclerView.Adapter<ListItemDeviceAdapter.ViewHolder> {
    private Activity activity;
    private ArrayList<Identity> identities;

    public ListItemDeviceAdapter(Activity activity, ArrayList<Identity> identities) {
        this.activity = activity;
        this.identities = identities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.item_custom_list_devices, parent, false);
        ViewHolder viewHolder =  new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Identity identity = (Identity) identities.get(position);
        holder.tvText.setText(identity.deviceId);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(activity instanceof MainActivity){
                    ((MainActivity) activity).switchToCameraView(identity);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return identities.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView tvText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tv_text);
        }
    }
}
