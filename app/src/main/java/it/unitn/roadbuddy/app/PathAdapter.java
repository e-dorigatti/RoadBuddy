package it.unitn.roadbuddy.app;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import it.unitn.roadbuddy.app.backend.models.Path;

/**
 * Created by BRomans on 07/06/2016.
 */
public class PathAdapter extends RecyclerView.Adapter<PathAdapter.MyViewHolder> {

    private List<Path> pathList;
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView path_id, owner, distance, duration;

        public MyViewHolder(View view) {
            super(view);
            path_id = (TextView) view.findViewById(R.id.path_id);
            owner = (TextView) view.findViewById(R.id.owner);
            distance = (TextView) view.findViewById(R.id.distance);
            duration = (TextView) view.findViewById(R.id.duration);
        }
    }
    public PathAdapter(List<Path> pathList){
        this.pathList = pathList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.trip_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Path path = pathList.get(position);
        holder.path_id.setText((int) path.getId());
        holder.owner.setText((int) path.getOwner());
        holder.distance.setText((int) path.getDistance());
        holder.duration.setText((int) path.getDuration());
    }


    @Override
    public int getItemCount() {
        return pathList.size();
    }
}
