package it.unitn.roadbuddy.app;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.gms.maps.GoogleMap;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.Trip;
import it.unitn.roadbuddy.app.backend.models.User;

public class NavigationState implements NFAState {

    Trip currentTrip;
    DrawablePath navigationPathDrawable;

    GoogleMap googleMap;
    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );
    MapFragment fragment;
    NFA nfa;

    @Override
    public void onStateEnter( final NFA nfa, final MapFragment fragment ) {
        if ( fragment.selectedDrawable == null ||
                !( fragment.selectedDrawable instanceof DrawablePath ) ) {

            AlertDialog.Builder builder = new AlertDialog.Builder( fragment.getActivity( ) );
            builder.setTitle( "Did you know?" );

            final TextView input = new TextView( fragment.getActivity( ) );
            input.setText( R.string.navigation_path_tip );
            builder.setView( input );

            builder.setPositiveButton(
                    R.string.navigation_path_tip_yes,
                    new DialogInterface.OnClickListener( ) {
                        @Override
                        public void onClick( DialogInterface dialog, int which ) {
                            fragment.showToast( R.string.navigation_path_tip_select );
                            nfa.Transition( new RestState( ) );
                        }
                    } );

            builder.setNegativeButton(
                    R.string.navigation_path_tip_no,
                    new DialogInterface.OnClickListener( ) {
                        @Override
                        public void onClick( DialogInterface dialog, int which ) {
                            dialog.cancel( );
                        }
                    } );

            builder.show( );
        }
        else navigationPathDrawable = ( DrawablePath ) fragment.selectedDrawable;

        this.fragment = fragment;
        this.nfa = nfa;
        this.googleMap = fragment.googleMap;
        this.googleMap.clear( );
    }

    @Override
    public void onStateExit( NFA nfa, MapFragment fragment ) {

    }

    class CreateTripAsync extends CancellableAsyncTask<Void, Void, Trip> {

        Path path;
        User currentUser;

        String exceptionMessage;

        public CreateTripAsync( Path path, User currentUser ) {
            super( taskManager );

            this.path = path;
            this.currentUser = currentUser;
        }

        @Override
        protected void onPreExecute( ) {
            ProgressBar pbar = new ProgressBar( fragment.getContext( ) );
            pbar.setIndeterminate( true );
            fragment.sliderLayout.setView( pbar );
        }

        @Override
        protected Trip doInBackground( Void... voids ) {
            try {
                return DAOFactory.getTripDAO( ).createTrip( path, currentUser );
            }
            catch ( BackendException exc ) {
                Log.e( getClass( ).getName( ), exc.getMessage( ), exc );
                exceptionMessage = exc.getMessage( );
                return null;
            }
        }

        @Override
        protected void onPostExecute( Trip res ) {
            if ( res == null ) {
                fragment.showToast( R.string.navigation_trip_creation_error );
                nfa.Transition( new RestState( ) );
            }
            else currentTrip = res;

            fragment.sliderLayout.setView( null );

            super.onPostExecute( res );
        }
    }
}
