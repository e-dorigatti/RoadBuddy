package it.unitn.roadbuddy.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DrawableCommentPOIInfoFragment extends SliderContentFragment {
    DrawableCommentPOI drawablePoi;

    public static DrawableCommentPOIInfoFragment newInstance( DrawableCommentPOI drawablePoi ) {
        DrawableCommentPOIInfoFragment f = new DrawableCommentPOIInfoFragment( );
        f.smallViewId = R.layout.fragment_drawable_poi_info_large;
        f.drawablePoi = drawablePoi;
        return f;
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {

        View view = super.onCreateView( inflater, container, savedInstanceState );

        TextView txtComment = ( TextView ) view.findViewById( R.id.txtComment );
        txtComment.setText( drawablePoi.getPOI( ).getText( ) );

        return view;
    }
}