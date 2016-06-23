package it.unitn.roadbuddy.app;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Random;

import it.unitn.roadbuddy.app.backend.models.Path;

public class TripsAdapter extends RecyclerView.Adapter<TripsAdapter.PathViewHolder> {

    private List<Path> pathList;

    private int randomNum;

    public TripsAdapter( List<Path> pathList ) {
        this.pathList = pathList;
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
    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
}