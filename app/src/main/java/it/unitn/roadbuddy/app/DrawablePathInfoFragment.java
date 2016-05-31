package it.unitn.roadbuddy.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DrawablePathInfoFragment extends DrawableInfoFragment {
    DrawablePath drawablePath;

    public static DrawablePathInfoFragment newInstance( DrawablePath drawablePath ) {
        DrawablePathInfoFragment f = new DrawablePathInfoFragment( );
        f.smallViewId = R.layout.basic_drawable_info;
        f.drawablePath = drawablePath;
        return f;
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {

        View view = super.onCreateView( inflater, container, savedInstanceState );

        TextView txtComment = ( TextView ) view.findViewById( R.id.txtComment );
        txtComment.setText( "asd" );

        return view;
    }
}