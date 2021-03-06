package it.unitn.roadbuddy.app;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.github.clans.fab.FloatingActionButton;
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

    private static final String SELECTED_POINT_KEY = "selected-point",
            COMMENT_KEY = "comment";

    LatLng selectedPoint;

    NFA nfa;
    MapFragment fragment;
    CommentPOI comment;
    DrawableCommentPOI drawable;
    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );

    @Override
    public void onStateEnter( final NFA nfa, final MapFragment fragment, Bundle savedInstanceState ) {
        this.fragment = fragment;
        this.nfa = nfa;

        fragment.googleMap.setOnMapLongClickListener( this );
        fragment.googleMap.setOnCameraChangeListener( this );
        fragment.googleMap.setOnMarkerClickListener( this );
        fragment.googleMap.setOnMapClickListener( this );

        LinearLayout buttonBar = ( LinearLayout ) fragment.mainLayout.setView( R.layout.button_layout_poi );
        FloatingActionButton btnAnnulla = ( FloatingActionButton ) buttonBar.findViewById( R.id.annulla );
        btnAnnulla.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                nfa.Transition( new RestState( ), null );
            }
        } );

        if ( selectedPoint != null )
            onMapLongClick( selectedPoint );
        else if ( comment != null )
            saveComment( );
        else fragment.showToast( R.string.long_tap_to_add );
    }

    @Override
    public void onStateExit( NFA nfa, MapFragment fragment ) {
        fragment.googleMap.setOnMapLongClickListener( null );
        fragment.googleMap.setOnCameraChangeListener( null );
        fragment.googleMap.setOnMapClickListener( null );
        fragment.googleMap.setOnMarkerClickListener( null );

        taskManager.stopRunningTasksOfType( SavePOIAsync.class );
        fragment.mainLayout.removeView( );
    }

    @Override
    public void onRestoreInstanceState( Bundle savedInstanceState ) {
        comment = ( CommentPOI ) savedInstanceState.getSerializable( COMMENT_KEY );

        SerializablePoint point = ( SerializablePoint )
                savedInstanceState.getSerializable( SELECTED_POINT_KEY );

        if ( point != null )
            selectedPoint = point.toLatLng( );
    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState ) {
        if ( selectedPoint != null )
            savedInstanceState.putSerializable( SELECTED_POINT_KEY,
                                                new SerializablePoint( selectedPoint ) );

        if ( comment != null )
            savedInstanceState.putSerializable( COMMENT_KEY, comment );
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
                selectedPoint = null;
                String text = input.getText( ).toString( );
                comment = new CommentPOI( 0, point.latitude, point.longitude, text,
                                          fragment.getCurrentUserId( ) );

                saveComment( );
            }
        } );

        builder.setNegativeButton( "Cancel", new DialogInterface.OnClickListener( ) {
            @Override
            public void onClick( DialogInterface dialog, int which ) {
                dialog.cancel( );
                fragment.showToast( "No point added..." );

                selectedPoint = null;
                comment = null;

                if ( drawable != null ) {
                    drawable.RemoveFromMap( fragment.getActivity( ) );
                }
            }
        } );

        selectedPoint = point;
        builder.show( );
    }

    @Override
    public boolean onMarkerClick( Marker m ) {
        return true;  // prevent the default behaviour
    }

    @Override
    public void onMapClick( LatLng point ) {
        //fragment.toggleMenuBar( );
    }

    void saveComment( ) {
        if ( !taskManager.isTaskRunning( SavePOIAsync.class ) ) {
            drawMarker( );
            taskManager.startRunningTask( new SavePOIAsync(
                    nfa, fragment.getActivity( ).getApplicationContext( )
            ), true, comment );
        }
        else fragment.showToast( R.string.wait_for_async_op_completion );
    }

    void drawMarker( ) {
        if ( comment != null ) {
            drawable = new DrawableCommentPOI( comment );
            drawable.DrawToMap( fragment.getContext( ), fragment.googleMap );
            drawable.setSelected( fragment.getContext( ), fragment.googleMap, true );
        }
    }

    @Override
    public void onCameraChange( final CameraPosition position ) {
        drawMarker( );
    }


    class SavePOIAsync extends CancellableAsyncTask<CommentPOI, Integer, Boolean> {

        NFA nfa;
        String exceptionMessage;
        Context context;

        public SavePOIAsync( NFA nfa, Context context ) {
            super( taskManager );
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
                Log.e( getClass( ).getName( ), "backend exception", exc );
                exceptionMessage = exc.getMessage( );
                return false;
            }
        }

        @Override
        protected void onPostExecute( Boolean success ) {
            if ( success ) {
                fragment.showToast( R.string.new_poi_saved );
                drawable.RemoveFromMap( context );
                fragment.RefreshMapContent( );
                nfa.Transition( new RestState( ), null );
            }
            else {
                if ( exceptionMessage != null ) {
                    fragment.showToast( exceptionMessage );
                }
                else {
                    fragment.showToast( R.string.generic_backend_error );
                }
            }

            super.onPostExecute( success );
        }
    }
}
