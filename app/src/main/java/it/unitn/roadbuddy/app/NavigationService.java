package it.unitn.roadbuddy.app;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Notification;
import it.unitn.roadbuddy.app.backend.models.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class NavigationService
        extends Service
        implements GoogleApiClient.ConnectionCallbacks {

    public static final int
            PARTICIPANTS_DATA = 1,
            NOTIFICATIONS_DATA = 2;

    public static final String
            ACTION_NAVIGATION = "navigation",
            ACTION_POSITION = "position",
            PARAM_USER = "userid",
            PARAM_TRIP = "tripid",
            PARAM_RECEIVER = "receiver",
            PARTICIPANTS_KEY = "participants",
            NOTIFICATIONS_KEY = "notifications";

    HandlerThread backgroundThread;
    Handler handler;
    GoogleApiClient googleApiClient;
    RefreshStatus refreshRunnable;
    PendingIntent locationUpdateIntent;

    public static void startNavigation( Context context, int tripId, int userId, ResultReceiver receiver ) {
        Intent intent = new Intent( context, NavigationService.class );
        intent.setAction( ACTION_NAVIGATION );
        intent.putExtra( PARAM_TRIP, tripId );
        intent.putExtra( PARAM_USER, userId );
        intent.putExtra( PARAM_RECEIVER, receiver );
        context.startService( intent );
    }

    public static void stopNavigation( Context context ) {
        Intent intent = new Intent( context, NavigationService.class );
        context.stopService( intent );
    }

    @Override
    public void onCreate( ) {
        super.onCreate( );

        backgroundThread = new HandlerThread( "background worker" );
        backgroundThread.start( );
        handler = new Handler( backgroundThread.getLooper( ) );

        googleApiClient = new GoogleApiClient.Builder( this )
                .addConnectionCallbacks( this )
                .addApi( LocationServices.API )
                .build( );

        googleApiClient.connect( );
    }

    @Override
    public void onDestroy( ) {
        super.onDestroy( );

        backgroundThread.quit( );

        if ( googleApiClient.isConnected( ) ) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    googleApiClient, locationUpdateIntent
            );

            googleApiClient.disconnect( );
        }
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId ) {
        if ( intent == null ) {
            stopSelf( );
            return START_STICKY;
        }

        String action = intent.getAction( );
        if ( ACTION_POSITION.equals( action ) && refreshRunnable != null ) {
            Location location = intent.getParcelableExtra( FusedLocationProviderApi.KEY_LOCATION_CHANGED );

            // obviously the assumption here is that we are managing a single user
            handler.removeCallbacks( refreshRunnable );
            refreshRunnable.setLocation( location );
            handler.post( refreshRunnable );
        }
        else if ( ACTION_NAVIGATION.equals( action ) ) {
            int tripId = intent.getIntExtra( PARAM_TRIP, -1 );
            int userId = intent.getIntExtra( PARAM_USER, -1 );
            ResultReceiver resultReceiver = intent.getParcelableExtra( PARAM_RECEIVER );

            refreshRunnable = new RefreshStatus( userId, tripId, resultReceiver );
            handler.post( refreshRunnable );
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind( Intent intent ) {
        return null;
    }

    @Override
    public void onConnectionSuspended( int i ) {

    }

    @Override
    public void onConnected( Bundle connectionHint ) {
        // called when the google api client has successfully connected to whatever

        // ask for periodic location updates running the listener on the background worker
        LocationRequest requestType = LocationRequest
                .create( )
                .setPriority( LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY )
                .setInterval( 60 * 1000 )
                .setFastestInterval( 15 * 1000 );

        Intent intent = new Intent( this, NavigationService.class );
        intent.setAction( ACTION_POSITION );

        locationUpdateIntent = PendingIntent.getService(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, requestType, locationUpdateIntent
        );
    }

    class RefreshStatus implements Runnable {
        public static final int INTERVAL = 15 * 1000;
        Set<Integer> receivedNotifications = new HashSet<>( );
        private int userId;
        private int tripId;
        private ResultReceiver receiver;
        private Location location;

        public RefreshStatus( int userId, int tripId, ResultReceiver receiver ) {
            this.userId = userId;
            this.tripId = tripId;
            this.receiver = receiver;
        }

        public void setLocation( Location location ) {
            this.location = location;
        }

        @Override
        public void run( ) {
            sendLocation( );

            retrieveParticipants( );

            retrieveNotifications( );

            handler.postDelayed( this, INTERVAL );
        }

        void sendLocation( ) {
            if ( location != null ) {
                try {
                    DAOFactory.getUserDAO( ).setCurrentLocation(
                            userId, new LatLng( location.getLatitude( ),
                                                location.getLongitude( ) )
                    );
                }
                catch ( BackendException exc ) {
                    Log.e( getClass( ).getName( ), "while updating user position", exc );
                }

                location = null;
            }
        }

        void retrieveParticipants( ) {
            if ( receiver == null )
                return;

            try {
                List<User> participants = DAOFactory.getUserDAO( ).getUsersOfTrip( tripId );

                Bundle data = new Bundle( );
                data.putParcelableArrayList(
                        PARTICIPANTS_KEY,
                        new ArrayList<>( participants )
                );


                receiver.send( PARTICIPANTS_DATA, data );
            }
            catch ( BackendException exc ) {
                Log.e( getClass( ).getName( ), "while retrieving trip participants", exc );
            }
        }

        void retrieveNotifications( ) {
            if ( receiver == null )
                return;

            try {
                List<Notification> notifications = DAOFactory.getNotificationDAO( ).getPings( tripId );

                ArrayList<Notification> unseen = new ArrayList<>( );
                for ( Notification not : notifications ) {
                    if ( !receivedNotifications.contains( not.getId( ) ) ) {
                        unseen.add( not );
                        receivedNotifications.add( not.getId( ) );
                    }
                }

                Bundle data = new Bundle( );
                data.putParcelableArrayList(
                        NOTIFICATIONS_KEY,
                        unseen
                );

                receiver.send( NOTIFICATIONS_DATA, data );
            }
            catch ( BackendException exc ) {
                Log.e( getClass( ).getName( ), "while retrieving trip participants", exc );
            }
        }
    }
}