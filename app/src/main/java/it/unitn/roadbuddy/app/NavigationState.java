package it.unitn.roadbuddy.app;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.*;
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
    RecyclerView rvParticipants;
    RecyclerView.Adapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    Button btnInviteBuddy;

    @Override
    public void onStateEnter( final NFA nfa, final MapFragment fragment ) {
        this.fragment = fragment;
        fragment.slidingLayout.setPanelState( SlidingUpPanelLayout.PanelState.COLLAPSED );
        this.nfa = nfa;
        this.googleMap = fragment.googleMap;

        fragment.clearMap( );

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

    void initUserInterface( ) {
        View buttons = fragment.mainLayout.setView( R.layout.button_layout_navigation );
        buttons.findViewById( R.id.btnInviteBuddy ).setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View view ) {
                inviteBuddy( );
            }
        } );

        buttons.findViewById( R.id.btnLeaveNavigation ).setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View view ) {
                nfa.Transition( new RestState( ) );
            }
        } );

        fragment.sliderLayout.setView( R.layout.navigation_layout );

        rvParticipants = ( RecyclerView ) fragment.getActivity( ).findViewById( R.id.rvTripParticipants );
        //this.emptyView = (TextView) fragment.getActivity().findViewById(R.id.empty_view);
        //rvParticipants.setEmptyView(emptyView);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager( fragment.getContext( ) );
        rvParticipants.setLayoutManager( mLayoutManager );
    }

    void inviteBuddy( ) {
        AlertDialog.Builder builder = new AlertDialog.Builder( fragment.getActivity( ) );
        builder.setTitle( R.string.navigation_invite );

        final EditText input = new EditText( fragment.getActivity( ) );
        input.setInputType( InputType.TYPE_CLASS_TEXT );
        builder.setView( input );

        builder.setPositiveButton(
                R.string.navigation_do_invite,
                new DialogInterface.OnClickListener( ) {
                    @Override
                    public void onClick( DialogInterface dialog, int which ) {
                        String invited = input.getText( ).toString( );
                        taskManager.startRunningTask( new SendInviteAsync( ), true, invited );
                    }
                } );

        builder.setNegativeButton(
                R.string.cancel,
                new DialogInterface.OnClickListener( ) {
                    @Override
                    public void onClick( DialogInterface dialog, int which ) {
                        dialog.cancel( );
                    }
                } );

        builder.show( );
    }

    @Override
    public void onStateExit( NFA nfa, MapFragment fragment ) {
        fragment.sliderLayout.setView( null );
        this.fragment.slidingLayout.setPanelState( SlidingUpPanelLayout.PanelState.HIDDEN );

        taskManager.stopRunningTasksOfType( CreateTripAsync.class );
        taskManager.stopRunningTasksOfType( SendInviteAsync.class );
    }

    class SendInviteAsync extends CancellableAsyncTask<String, Void, Boolean> {

        String exceptionMessage;

        public SendInviteAsync( ) {
            super( taskManager );
        }

        @Override
        protected Boolean doInBackground( String... userName ) {
            try {
                return DAOFactory.getInviteDAO( ).addInvite(
                        fragment.currentUser.getId( ), userName[ 0 ], currentTrip.getId( )
                );
            }
            catch ( BackendException exc ) {
                Log.e( getClass( ).getName( ), "while sending invite", exc );
                exceptionMessage = exc.getMessage( );
                return null;
            }
        }

        @Override
        protected void onPostExecute( Boolean res ) {
            int message;

            if ( res == null ) {
                message = R.string.generic_backend_error;
            }
            else if ( res ) {
                message = R.string.navigation_invite_sent;
            }
            else {
                message = R.string.navigation_invite_fail;
            }

            fragment.showToast( message );

            super.onPostExecute( res );
        }
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
            pbar.setLayoutParams( new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, 150
            ) );
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
            else {
                currentTrip = res;
                initUserInterface( );
            }

            super.onPostExecute( res );
        }
    }
}
