package it.unitn.roadbuddy.app;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.Trip;
import it.unitn.roadbuddy.app.backend.models.User;

import java.util.ArrayList;
import java.util.List;

public class NavigationState implements NFAState,
                                        NavigationInfoFragment.ParticipantInteractionListener,
                                        GoogleMap.OnMarkerClickListener,
                                        GoogleMap.OnMapClickListener {
    Trip currentTrip;
    DrawablePath navigationPathDrawable;
    GoogleMap googleMap;
    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );
    MapFragment fragment;
    NFA nfa;
    List<User> buddies = new ArrayList<>( );
    List<DrawableUser> drawableUsers = new ArrayList<>( );
    DrawableUser selectedUser;
    /**
     * used when we are invited to a trip
     * <p/>
     * this state can be "invoked" in two different ways:
     * - by providing a non-null trip id, in which case the user was invited
     * to join that trip by one of his buddies
     * - by providing a null trip id, in which case the user has just created
     * the trip
     */
    Integer invitationTrip;
    String inviterName;
    RefreshBuddiesRunnable buddiesRefresh;
    NavigationInfoFragment infoFragment;

    public NavigationState( Integer invitationTrip, String inviterName ) {
        this.invitationTrip = invitationTrip;
        this.inviterName = inviterName;
    }

    @Override
    public void onStateEnter( final NFA nfa, final MapFragment fragment, Bundle savedInstanceStat ) {
        this.fragment = fragment;
        this.nfa = nfa;
        this.googleMap = fragment.googleMap;

        fragment.slidingLayout.setPanelState( SlidingUpPanelLayout.PanelState.COLLAPSED );
        fragment.clearMap( );

        googleMap.setOnMarkerClickListener( this );
        googleMap.setOnMapClickListener( this );
        googleMap.setMyLocationEnabled( false );

        if ( invitationTrip == null )
            newTrip( );
        else handleInvite( );
    }

    @Override
    public void onStateExit( NFA nfa, MapFragment fragment ) {
        buddiesRefresh.Stop( );
        fragment.sliderLayout.setView( null );
        this.fragment.slidingLayout.setPanelState( SlidingUpPanelLayout.PanelState.HIDDEN );

        googleMap.setOnMarkerClickListener( this );
        googleMap.setOnMapClickListener( this );

        if ( fragment.mainActivity.isLocationPermissionEnabled( ) )
            googleMap.setMyLocationEnabled( true );

        for ( DrawableUser d : drawableUsers )
            d.RemoveFromMap( fragment.getContext( ) );

        taskManager.stopAllRunningTasks( );
    }

    @Override
    public void onRestoreInstanceState( Bundle savedInstanceState ) {

    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState ) {

    }

    @Override
    public void onMapClick( LatLng latLng ) {
        if ( selectedUser != null ) {
            selectedUser.setSelected( fragment.getContext( ), googleMap, false );
            selectedUser = null;
        }
        infoFragment.setSelectedUser( null );
    }

    @Override
    public boolean onMarkerClick( Marker marker ) {
        if ( selectedUser != null )
            selectedUser.setSelected( fragment.getContext( ), googleMap, false );

        for ( DrawableUser d : drawableUsers ) {
            if ( !d.getMapId( ).equals( marker.getId( ) ) )
                continue;

            d.setSelected( fragment.getContext( ), googleMap, true );
            selectedUser = d;
            infoFragment.setSelectedUser( selectedUser.getUser( ) );
            moveCameraTo( selectedUser.getUser( ).getLastPosition( ), 15 );
            break;
        }

        return false;
    }

    @Override
    public void onParticipantSelected( User participant ) {
        if ( participant != null && participant.getLastPosition( ) != null )
            moveCameraTo( participant.getLastPosition( ), 15 );
    }

    void moveCameraTo( LatLng point, float zoom ) {
        CameraUpdate anim = CameraUpdateFactory.newLatLngZoom( point, zoom );
        fragment.slidingLayout.setPanelState( SlidingUpPanelLayout.PanelState.COLLAPSED );
        googleMap.animateCamera( anim );
    }

    // called when the user has been invited to join a trip
    void handleInvite( ) {
        AlertDialog.Builder builder = new AlertDialog.Builder( fragment.getActivity( ) );
        builder.setTitle( R.string.navigation_join_confirm_title );

        final TextView input = new TextView( fragment.getActivity( ) );
        input.setText( String.format(
                fragment.getString( R.string.navigation_join_confirm_text ),
                inviterName
        ) );

        builder.setView( input );

        builder.setPositiveButton(
                R.string.yes,
                new DialogInterface.OnClickListener( ) {
                    @Override
                    public void onClick( DialogInterface dialog, int which ) {
                        taskManager.startRunningTask( new JoinTripAsync( ), true, invitationTrip );
                    }
                } );

        builder.setNegativeButton(
                R.string.no,
                new DialogInterface.OnClickListener( ) {
                    @Override
                    public void onClick( DialogInterface dialog, int which ) {
                        // the invite was already removed when the notification was sent
                        fragment.getActivity( ).finish( );
                    }
                } );

        builder.show( );
    }

    // called when the user is creating a new trip
    void newTrip( ) {
        if ( fragment.selectedDrawable == null ||
                !( fragment.selectedDrawable instanceof DrawablePath ) ) {

            AlertDialog.Builder builder = new AlertDialog.Builder( fragment.getActivity( ) );
            builder.setTitle( "Did you know?" );

            final TextView input = new TextView( fragment.getActivity( ) );
            input.setText( R.string.navigation_path_tip );
            input.setTextAlignment( View.TEXT_ALIGNMENT_CENTER );
            builder.setView( input );

            builder.setPositiveButton(
                    R.string.navigation_path_tip_yes,
                    new DialogInterface.OnClickListener( ) {
                        @Override
                        public void onClick( DialogInterface dialog, int which ) {
                            fragment.showToast( R.string.navigation_path_tip_select );
                            nfa.Transition( new RestState( ), null );
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
                                    new CreateTripAsync( null, fragment.getCurrentUser( ) ),
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
                    new CreateTripAsync( navigationPathDrawable.getPath( ), fragment.getCurrentUser( ) ),
                    true
            );
        }
    }

    // called when the user is ready to start a trip (either joined or new one)
    void startTrip( ) {
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
                taskManager.startRunningTask( new AbandonTripAsync( ), true );
            }
        } );

        if ( navigationPathDrawable != null ) {
            navigationPathDrawable.DrawToMap( fragment.getContext( ), googleMap );
            navigationPathDrawable.setSelected( fragment.getContext( ), googleMap, true );
        }

        infoFragment = NavigationInfoFragment.newInstance(
                fragment.getCurrentUserId( ),
                navigationPathDrawable
        );
        infoFragment.setParticipantInteractionListener( this );
        fragment.sliderLayout.setFragment( infoFragment );

        buddiesRefresh = new RefreshBuddiesRunnable( fragment.mainActivity.backgroundTasksHandler );
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

    // update the info about buddies such as position and distance from user
    void updateBuddies( List<User> buddies ) {
        this.buddies = buddies;
        infoFragment.setBuddies( buddies );

        for ( Drawable d : drawableUsers ) {
            d.RemoveFromMap( fragment.getContext( ) );
        }
        drawableUsers.clear( );
        for ( User u : buddies ) {
            DrawableUser drawable = new DrawableUser( u );
            drawable.DrawToMap( fragment.getContext( ), googleMap );
            drawableUsers.add( drawable );
        }
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
                        fragment.getCurrentUserId( ), userName[ 0 ], currentTrip.getId( )
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

    class JoinTripAsync extends CancellableAsyncTask<Integer, Void, Trip> {
        public JoinTripAsync( ) {
            super( taskManager );
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
        protected Trip doInBackground( Integer... tripId ) {
            try {
                DAOFactory.getUserDAO( ).joinTrip(
                        fragment.getCurrentUserId( ), tripId[ 0 ]
                );

                return DAOFactory.getTripDAO( ).getTrip( tripId[ 0 ] );
            }
            catch ( BackendException exc ) {
                Log.e( getClass( ).getName( ), "while joining trip", exc );
                return null;
            }
        }

        @Override
        protected void onPostExecute( Trip res ) {
            if ( res != null ) {
                currentTrip = res;
                if ( currentTrip.getPath( ) != null )
                    navigationPathDrawable = new DrawablePath( currentTrip.getPath( ) );
                startTrip( );
            }
            else {
                fragment.showToast( R.string.generic_backend_error );
                fragment.getActivity( ).finish( );
            }

            super.onPostExecute( res );
        }
    }

    class AbandonTripAsync extends CancellableAsyncTask<Void, Void, Boolean> {
        public AbandonTripAsync( ) {
            super( taskManager );
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
        protected Boolean doInBackground( Void... nothing ) {
            try {
                DAOFactory.getUserDAO( ).joinTrip(
                        fragment.getCurrentUserId( ), null
                );
                return true;
            }
            catch ( BackendException exc ) {
                Log.e( getClass( ).getName( ), "while joining trip", exc );
                return false;
            }
        }

        @Override
        protected void onPostExecute( Boolean res ) {
            super.onPostExecute( res );
            nfa.Transition( new RestState( ), null );
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
                nfa.Transition( new RestState( ), null );
            }
            else {
                currentTrip = res;
                startTrip( );
            }

            super.onPostExecute( res );
        }
    }

    class RefreshBuddiesRunnable implements Runnable {

        public static final int REFRESH_INTERVAL = 15 * 1000;

        Handler handler;

        public RefreshBuddiesRunnable( Handler handler ) {
            this.handler = handler;

            handler.post( this );
        }

        @Override
        public void run( ) {
            try {
                final List<User> participants = DAOFactory.getUserDAO( ).getUsersOfTrip(
                        currentTrip.getId( )
                );

                /**
                 * By forcing the update to happen on the UI thread we are sure
                 * to avoid concurrency issues and don't need to add explicit
                 * synchronization to the rest of the code.
                 */
                fragment.getActivity( ).runOnUiThread( new Runnable( ) {
                    @Override
                    public void run( ) {
                        updateBuddies( participants );
                    }
                } );
            }
            catch ( BackendException exc ) {
                Log.e( getClass( ).getName( ), "while getting users of trip", exc );
            }

            handler.postDelayed( this, REFRESH_INTERVAL );
        }

        public void Stop( ) {
            handler.removeCallbacks( this );
        }
    }
}
