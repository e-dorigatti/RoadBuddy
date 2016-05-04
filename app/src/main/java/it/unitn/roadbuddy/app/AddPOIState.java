package it.unitn.roadbuddy.app;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.CommentPOI;

public class AddPOIState implements NFAState,
                                    GoogleMap.OnMapClickListener,
                                    GoogleMap.OnMapLongClickListener,
                                    GoogleMap.OnCameraChangeListener {

    MainActivity activity;
    CommentPOI comment;
    Marker marker;
    LinearLayout buttonBar;
    Button btnOk;
    Button btnCancel;

    @Override
    public void onStateEnter( final NFA nfa, final MainActivity activity ) {
        this.activity = activity;

        activity.map.setOnMapLongClickListener( this );
        activity.map.setOnCameraChangeListener( this );
        activity.map.setOnMapClickListener( this );

        activity.showToast( "Long tap to add" );

        buttonBar = ( LinearLayout ) activity.setMenuBar( R.layout.ok_cancel_layout );
        buttonBar.setVisibility( View.INVISIBLE );

        btnOk = ( Button ) buttonBar.findViewById( R.id.btnOk );
        btnOk.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                if ( comment != null ) {
                    DAOFactory.getPoiDAOFactory( ).getCommentPoiDAO( ).AddCommentPOI(
                            activity.getApplicationContext( ), comment
                    );

                    activity.showToast( "Point saved" );
                }
                else activity.showToast( "No point added..." );

                nfa.Transition( new RestState( ) );
            }
        } );

        btnCancel = ( Button ) buttonBar.findViewById( R.id.btnCancel );
        btnCancel.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                activity.showToast( "No point added..." );
                nfa.Transition( new RestState( ) );
            }
        } );
    }

    @Override
    public void onStateExit( NFA nfa, MainActivity activity ) {
        activity.map.setOnMapLongClickListener( null );
        activity.map.setOnCameraChangeListener( null );
        activity.map.setOnMapClickListener( null );

        activity.removeMenuBar( );
    }

    @Override
    public void onMapLongClick( final LatLng point ) {
        AlertDialog.Builder builder = new AlertDialog.Builder( activity );
        builder.setTitle( "Enter text" );

        final EditText input = new EditText( activity );
        input.setInputType( InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT );
        builder.setView( input );

        builder.setPositiveButton( "Add", new DialogInterface.OnClickListener( ) {
            @Override
            public void onClick( DialogInterface dialog, int which ) {
                if ( marker != null ) {
                    marker.remove( );
                }

                String text = input.getText( ).toString( );
                comment = new CommentPOI( 0, point.latitude, point.longitude, text );

                drawMarker( );
            }
        } );

        builder.setNegativeButton( "Cancel", new DialogInterface.OnClickListener( ) {
            @Override
            public void onClick( DialogInterface dialog, int which ) {
                dialog.cancel( );
            }
        } );

        builder.show( );
    }

    @Override
    public void onMapClick( LatLng point ) {
        activity.toggleMenuBar( );
    }

    void drawMarker( ) {
        if ( comment != null ) {
            MarkerOptions options = comment.drawToMap( activity.map );
            marker = activity.map.addMarker( options );
            marker.showInfoWindow( );
        }
    }

    public void onCameraChange( final CameraPosition position ) {
        activity.RefreshMapContent( );
        drawMarker( );
    }
}
