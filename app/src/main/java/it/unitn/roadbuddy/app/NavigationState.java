package it.unitn.roadbuddy.app;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.gms.maps.GoogleMap;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
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
    EmptyRecyclerView mEmptyRecyclerView;
    RecyclerView.Adapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;
    TextView emptyView;

    Button addBuddy;

    @Override
    public void onStateEnter( final NFA nfa, final MapFragment fragment ) {
        this.fragment = fragment;
        fragment.slidingLayout.setPanelState( SlidingUpPanelLayout.PanelState.COLLAPSED );
        this.nfa = nfa;
        this.googleMap = fragment.googleMap;

        fragment.clearMap( );

        fragment.sliderLayout.setView( R.layout.navigation_layout );
        addBuddy = ( Button ) fragment.getActivity( ).findViewById( R.id.addBuddy );
        mEmptyRecyclerView = ( EmptyRecyclerView ) fragment.getActivity( ).findViewById( R.id.my_navigation_recycler_view );
        //this.emptyView = (TextView) fragment.getActivity().findViewById(R.id.empty_view);
        //mEmptyRecyclerView.setEmptyView(emptyView);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager( fragment.getContext( ) );
        mEmptyRecyclerView.setLayoutManager( mLayoutManager );

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
                            navigationPathDrawable = null;
                            taskManager.startRunningTask(
                                    new CreateTripAsync( null, fragment.currentUser ),
                                    true
                            );
                        }
                    } );

            builder.show( );
        }
        else {
            navigationPathDrawable = ( DrawablePath ) fragment.selectedDrawable;
            navigationPathDrawable.setSelected( fragment.getContext( ), googleMap, true );
            taskManager.startRunningTask(
                    new CreateTripAsync( navigationPathDrawable.getPath( ), fragment.currentUser ),
                    true
            );
        }
    }

    @Override
    public void onStateExit( NFA nfa, MapFragment fragment ) {
        this.fragment.slidingLayout.setPanelState( SlidingUpPanelLayout.PanelState.HIDDEN );
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
