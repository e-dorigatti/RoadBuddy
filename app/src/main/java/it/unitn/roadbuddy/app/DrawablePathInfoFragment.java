package it.unitn.roadbuddy.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Random;

import it.unitn.roadbuddy.app.backend.models.Path;

public class DrawablePathInfoFragment extends Fragment {

    DrawablePath drawablePath;
    RelativeLayout mainLayout;
    private int randomNum;

    public static DrawablePathInfoFragment newInstance( DrawablePath drawablePath ) {
        DrawablePathInfoFragment f = new DrawablePathInfoFragment( );
        f.drawablePath = drawablePath;
        return f;
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        randomNum = getRandomNumberInRange(1,4);
        mainLayout = (RelativeLayout) inflater.inflate(
                R.layout.fragment_drawable_path_info_large, container, false
        );
       // ImageView vImg = (ImageView) mainLayout.findViewById(R.id.card_image_slider);
        TextView txtDuration = ( TextView ) mainLayout.findViewById( R.id.txtTotalDuration );
        TextView txtDistance = ( TextView ) mainLayout.findViewById( R.id.txtTotalDistance );
        TextView txtDescription = ( TextView ) mainLayout.findViewById( R.id.txtPathDescription );


        if ( drawablePath != null ) {
            txtDistance.setText(
                    String.format( getString( R.string.path_edit_total_distance ),
                                   Path.formatDistance( drawablePath.getPath( ).getDistance( ) )
                    ) );

            txtDuration.setText(
                    String.format( getString( R.string.path_edit_total_duration ),
                                   Path.formatDuration( drawablePath.getPath( ).getDuration( ) )
                    ) );

            txtDescription.setText( drawablePath.getPath( ).getDescription( ) );
            txtDescription.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            txtDescription.setSelected(true);
            txtDescription.setSingleLine(true);
        }

        return mainLayout;
    }
    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
}