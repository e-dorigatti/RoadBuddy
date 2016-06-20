package it.unitn.roadbuddy.app;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import it.unitn.roadbuddy.app.backend.models.Path;

public class TripsAdapter extends RecyclerView.Adapter<TripsAdapter.PathViewHolder> {

    private List<Path> pathList;

    public TripsAdapter( List<Path> pathList ) {
        this.pathList = pathList;
    }

    @Override
    public TripsAdapter.PathViewHolder onCreateViewHolder( ViewGroup parent, int viewType ) {
        View itemView = LayoutInflater.from( parent.getContext( ) ).inflate( R.layout.trip_list_row, parent, false );

        return new PathViewHolder( itemView );
    }

    @Override
    public void onBindViewHolder( TripsAdapter.PathViewHolder holder, int position ) {
        Path pi = pathList.get( position );
        holder.vId.setText(  Long.toString(pi.getId())+ ") " + pi.getDescription() );
        holder.vOwner.setText( "Owner: " + Long.toString( pi.getOwner( ) ) );
        holder.vDistance.setText(String.format( "Total Distance: %s",
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
        protected TextView vId;
        protected TextView vOwner;
        protected TextView vDuration;
        protected TextView vDistance;

        public PathViewHolder( View itemView ) {
            super( itemView );
            vId = ( TextView ) itemView.findViewById( R.id.path_id );
            vOwner = ( TextView ) itemView.findViewById( R.id.path_owner );
            vDuration = ( TextView ) itemView.findViewById( R.id.path_duration );
            vDistance = ( TextView ) itemView.findViewById( R.id.path_distance );
        }
    }

    public Path getPath(int position) {
        return pathList.get(position);
    }
}