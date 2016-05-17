package it.unitn.roadbuddy.app;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.CommentPOI;

public class AddPOIState implements NFAState,
                                    GoogleMap.OnMapClickListener,
                                    GoogleMap.OnMapLongClickListener,
                                    GoogleMap.OnMarkerClickListener,
                                    GoogleMap.OnCameraChangeListener {

    MapFragment fragment;
    CommentPOI comment;
    DrawableCommentPOI drawable;
    LinearLayout buttonBar;
    Button btnOk;
    Button btnCancel;
    SavePOIAsync asyncSave;

    @Override
    public void onStateEnter( final NFA nfa, final MapFragment fragment ) {
        this.fragment = fragment;

        fragment.googleMap.setOnMapLongClickListener( this );
        fragment.googleMap.setOnCameraChangeListener( this );
        fragment.googleMap.setOnMarkerClickListener( this );
        fragment.googleMap.setOnMapClickListener( this );

        fragment.showToast( R.string.long_tap_to_add );

        buttonBar = ( LinearLayout ) fragment.setCurrentMenuBar( R.layout.ok_cancel_layout );
        buttonBar.setVisibility( View.INVISIBLE );

        btnOk = ( Button ) buttonBar.findViewById( R.id.btnOk );
        btnOk.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                if ( comment != null ) {
                    if ( asyncSave == null ) {
                        asyncSave = new SavePOIAsync(
                                nfa, fragment.getActivity( ).getApplicationContext( )
                        );

                        asyncSave.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, comment );
                    }
                    else fragment.showToast( R.string.wait_for_network );
                }
                else {
                    fragment.showToast( R.string.new_poi_cancel );
                    nfa.Transition( new RestState( ) );
                }
            }
        } );

        btnCancel = ( Button ) buttonBar.findViewById( R.id.btnCancel );
        btnCancel.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                fragment.showToast( "No point added..." );
                nfa.Transition( new RestState( ) );
            }
        } );
    }

    @Override
    public void onStateExit( NFA nfa, MapFragment fragment ) {
        fragment.googleMap.setOnMapLongClickListener( null );
        fragment.googleMap.setOnCameraChangeListener( null );
        fragment.googleMap.setOnMapClickListener( null );
        fragment.googleMap.setOnMarkerClickListener( null );

        fragment.removeMenuBar( );
    }

    @Override
    public void onMapLongClick( final LatLng point ) {
        AlertDialog.Builder builder = new AlertDialog.Builder( fragment.getActivity( ) );
        builder.setTitle( "Enter text" );

        final EditText input = new EditText( fragment.getActivity( ) );
        input.setInputType( InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT );
        builder.setView( input );

        builder.setPositiveButton( "Add", new DialogInterface.OnClickListener( ) {
            @Override
            public void onClick( DialogInterface dialog, int which ) {
                if ( drawable != null ) {
                    drawable.RemoveFromMap( );
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
    public boolean onMarkerClick( Marker m ) {
        return true;  // prevent the default behaviour
    }

    @Override
    public void onMapClick( LatLng point ) {
        fragment.toggleMenuBar( );
    }

    void drawMarker( ) {
        if ( comment != null ) {
            drawable = new DrawableCommentPOI( comment );
            drawable.DrawToMap( fragment.googleMap );
            drawable.setSelected( true );
        }
    }

    @Override
    public void onCameraChange( final CameraPosition position ) {
        fragment.RefreshMapContent( );
        drawMarker( );
    }


    class SavePOIAsync extends AsyncTask<CommentPOI, Integer, Boolean> {

        NFA nfa;
        String exceptionMessage;
        Context context;

        public SavePOIAsync( NFA nfa, Context context ) {
            this.nfa = nfa;
            this.context = context;
        }

        @Override
        protected Boolean doInBackground( CommentPOI... poi ) {
            try {
                DAOFactory.getPoiDAOFactory( ).getCommentPoiDAO( ).AddCommentPOI(
                        context, poi[ 0 ]
                );
                return true;
            }
            catch ( BackendException exc ) {
                Log.e( "roadbuddy", "backend exception", exc );
                exceptionMessage = exc.getMessage( );
                return false;
            }
        }

        @Override
        protected void onPostExecute( Boolean success ) {
            if ( success ) {
                fragment.showToast( R.string.new_poi_saved );
                nfa.Transition( new RestState( ) );
            }
            else {
                if ( exceptionMessage != null ) {
                    fragment.showToast( exceptionMessage );
                }
                else {
                    fragment.showToast( R.string.generic_backend_error );
                }
            }

            asyncSave = null;
        }
    }
}
