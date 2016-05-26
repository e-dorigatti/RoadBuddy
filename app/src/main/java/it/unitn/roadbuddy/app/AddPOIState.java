package it.unitn.roadbuddy.app;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
    NFA nfa;
    MapFragment fragment;
    CommentPOI comment;
    DrawableCommentPOI drawable;
    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );

    @Override
    public void onStateEnter( NFA nfa, final MapFragment fragment ) {
        this.fragment = fragment;
        this.nfa = nfa;

        fragment.googleMap.setOnMapLongClickListener( this );
        fragment.googleMap.setOnCameraChangeListener( this );
        fragment.googleMap.setOnMarkerClickListener( this );
        fragment.googleMap.setOnMapClickListener( this );

        fragment.showToast( R.string.long_tap_to_add );

    }

    @Override
    public void onStateExit( NFA nfa, MapFragment fragment ) {
        fragment.googleMap.setOnMapLongClickListener( null );
        fragment.googleMap.setOnCameraChangeListener( null );
        fragment.googleMap.setOnMapClickListener( null );
        fragment.googleMap.setOnMarkerClickListener( null );

        fragment.removeMenuBar( );
        taskManager.stopRunningTask( SavePOIAsync.class );
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

                String text = input.getText( ).toString( );
                comment = new CommentPOI( 0, point.latitude, point.longitude, text,
                                          fragment.currentUser.getId( ) );
                drawMarker( );

                if ( !taskManager.isTaskRunning( SavePOIAsync.class ) ) {
                    taskManager.startRunningTask( new SavePOIAsync(
                            nfa, fragment.getActivity( ).getApplicationContext( )
                    ), true, comment );
                }
                else fragment.showToast( R.string.wait_for_async_op_completion );
            }
        } );

        builder.setNegativeButton( "Cancel", new DialogInterface.OnClickListener( ) {
            @Override
            public void onClick( DialogInterface dialog, int which ) {
                dialog.cancel( );
                fragment.showToast( "No point added..." );
                if(drawable!= null){
                    drawable.RemoveFromMap(fragment.getActivity());
                }
                nfa.Transition( new RestState( ) );
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
        //fragment.toggleMenuBar( );
    }

    void drawMarker( ) {
        if ( comment != null ) {
            drawable = new DrawableCommentPOI( comment );
            drawable.DrawToMap( fragment.getContext( ), fragment.googleMap );
            drawable.setSelected( fragment.getContext( ), true );
        }
    }

    @Override
    public void onCameraChange( final CameraPosition position ) {
        fragment.RefreshMapContent( );
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

            super.onPostExecute( success );
        }
    }
}
