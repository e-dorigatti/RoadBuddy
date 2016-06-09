package it.unitn.roadbuddy.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import it.unitn.roadbuddy.app.backend.models.Path;

public class DrawablePathInfoFragment extends SliderContentFragment {
    DrawablePath drawablePath;

    public static DrawablePathInfoFragment newInstance( DrawablePath drawablePath ) {
        DrawablePathInfoFragment f = new DrawablePathInfoFragment( );
        f.smallViewId = R.layout.fragment_drawable_path_info_large;
        f.drawablePath = drawablePath;
        return f;
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {

        View view = super.onCreateView( inflater, container, savedInstanceState );

        TextView txtDuration = ( TextView ) view.findViewById( R.id.txtTotalDuration );
        TextView txtDistance = ( TextView ) view.findViewById( R.id.txtTotalDistance );
        TextView txtDescription = ( TextView ) view.findViewById( R.id.txtPathDescription );

        txtDistance.setText(
                String.format( getString( R.string.path_edit_total_distance ),
                               Path.formatDistance( drawablePath.getPath( ).getDistance( ) )
                ) );

        txtDuration.setText(
                String.format( getString( R.string.path_edit_total_duration ),
                               Path.formatDuration( drawablePath.getPath( ).getDuration( ) )
                ) );

        txtDescription.setText( drawablePath.getPath( ).getDescription( ) );

        return view;
    }
}