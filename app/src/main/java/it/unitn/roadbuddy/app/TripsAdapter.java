package it.unitn.roadbuddy.app;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import it.unitn.roadbuddy.app.backend.models.Path;

public class TripsAdapter extends RecyclerView.Adapter<TripsAdapter.PathViewHolder> {

    private List<Path> pathList;

    private int randomNum;

    public TripsAdapter( List<Path> pathList ) {

        this.pathList = new ArrayList<>(pathList);
    }

    @Override
    public TripsAdapter.PathViewHolder onCreateViewHolder( ViewGroup parent, int viewType ) {
        View itemView = LayoutInflater.from( parent.getContext( ) ).inflate( R.layout.trip_list_row, parent, false );
        randomNum = getRandomNumberInRange(1,4);
        return new PathViewHolder( itemView );
    }

    @Override
    public void onBindViewHolder( TripsAdapter.PathViewHolder holder, int position ) {
        Path pi = pathList.get( position );

        switch (randomNum){
            case 1:
                holder.vImg.setImageResource(R.drawable.sample_background);
                break;
            case 2:
                holder.vImg.setImageResource(R.drawable.sample_background2);
                break;
            case 3:
                holder.vImg.setImageResource(R.drawable.sample_background3);
                break;
            case 4:
                holder.vImg.setImageResource(R.drawable.sample_background4);
                break;
        }
        holder.vId.setText( pi.getDescription() );
        holder.vOwner.setText( "User: " + Long.toString( pi.getOwner( ) ) );
        holder.vDistance.setText(String.format( "Distance: %s",
                        Path.formatDistance( pi.getDistance( ) )
                ) );
        holder.vDuration.setText(String.format( "Expected Duration: %s",
                        Path.formatDuration( pi.getDuration( ) )
                ) );
    }

    @Override
    public int getItemCount( ) {
        return pathList != null ? pathList.size( ) : 0;
    }

    public static class PathViewHolder extends RecyclerView.ViewHolder {
        protected ImageView vImg;
        protected TextView vId;
        protected TextView vOwner;
        protected TextView vDuration;
        protected TextView vDistance;

        public PathViewHolder( View itemView ) {
            super( itemView );
            vImg = (ImageView) itemView.findViewById(R.id.card_image);
            vId = ( TextView ) itemView.findViewById( R.id.path_id );
            vOwner = ( TextView ) itemView.findViewById( R.id.path_owner );
            vDuration = ( TextView ) itemView.findViewById( R.id.path_duration );
            vDistance = ( TextView ) itemView.findViewById( R.id.path_distance );
        }
    }

    public Path getPath(int position) {

        return pathList.get(position);
    }

    public Path removeItem(int position) {
        final Path path = pathList.remove(position);
        notifyItemRemoved(position);
        return path;
    }

    public void addItem(int position, Path path) {
        pathList.add(position, path);
        notifyItemInserted(position);
    }

    public void moveItem(int fromPosition, int toPosition) {
        final Path path = pathList.remove(fromPosition);
        pathList.add( toPosition, path );
        notifyItemMoved( fromPosition, toPosition );
    }

    public void animateTo(List<Path> paths) {
        applyAndAnimateRemovals(paths);
        applyAndAnimateAdditions(paths);
        applyAndAnimateMovedItems(paths);
    }

    private void applyAndAnimateRemovals(List<Path> newPaths) {
        for (int i = pathList.size() - 1; i >= 0; i--) {
            final Path path = pathList.get(i);
            if (!newPaths.contains(path)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<Path> newPaths) {
        for (int i = 0, count = newPaths.size(); i < count; i++) {
            final Path path = newPaths.get(i);
            if (!pathList.contains(path)) {
                addItem(i, path);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<Path> newPaths) {
        for (int toPosition = newPaths.size() - 1; toPosition >= 0; toPosition--) {
            final Path path = newPaths.get(toPosition);
            final int fromPosition = pathList.indexOf(path);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
}