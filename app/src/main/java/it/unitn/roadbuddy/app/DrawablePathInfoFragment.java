package it.unitn.roadbuddy.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import it.unitn.roadbuddy.app.backend.models.Path;

public class DrawablePathInfoFragment extends Fragment {

    DrawablePath drawablePath;
    LinearLayout mainLayout;

    public static DrawablePathInfoFragment newInstance( DrawablePath drawablePath ) {
        DrawablePathInfoFragment f = new DrawablePathInfoFragment( );
        f.drawablePath = drawablePath;
        return f;
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {

        mainLayout = ( LinearLayout ) inflater.inflate(
                R.layout.fragment_drawable_path_info_large, container, false
        );

        TextView txtDuration = ( TextView ) mainLayout.findViewById( R.id.txtTotalDuration );
        TextView txtDistance = ( TextView ) mainLayout.findViewById( R.id.txtTotalDistance );
        TextView txtDescription = ( TextView ) mainLayout.findViewById( R.id.txtPathDescription );

        txtDistance.setText(
                String.format( getString( R.string.path_edit_total_distance ),
                               Path.formatDistance( drawablePath.getPath( ).getDistance( ) )
                ) );

        txtDuration.setText(
                String.format( getString( R.string.path_edit_total_duration ),
                               Path.formatDuration( drawablePath.getPath( ).getDuration( ) )
                ) );

        txtDescription.setText( drawablePath.getPath( ).getDescription( ) );

        return mainLayout;
    }
}