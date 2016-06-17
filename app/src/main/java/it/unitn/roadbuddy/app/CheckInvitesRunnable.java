package it.unitn.roadbuddy.app;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Invite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CheckInvitesRunnable implements Runnable {
    public final static int INTERVAL = 15 * 1000;

    Context context;
    Handler handler;
    int userId;

    Set<Integer> notifications = new HashSet<>( );

    public CheckInvitesRunnable( Handler handler, Context context, int userId ) {
        this.handler = handler;
        this.userId = userId;
        this.context = context;

        handler.postDelayed( this, INTERVAL );
    }

    @Override
    public void run( ) {
        List<Invite> invites;

        try {
            invites = DAOFactory.getInviteDAO( ).retrieveInvites( userId );
        }
        catch ( BackendException exc ) {
            invites = null;
        }

        if ( invites != null ) {
            for ( Invite invite : invites ) {
                // do not bother the user with repeated notifications
                // TODO group multiple invites into a single notification
                // [ed] I don't think this is necessary as it is unlikely to
                // receive multiple invites in a short time span
                if ( !notifications.contains( invite.getId( ) ) ) {
                    notifications.add( invite.getId( ) );
                    sendNotification( invite );
                }
            }
        }

        handler.postDelayed( this, INTERVAL );
    }

    void sendNotification( Invite invite ) {
        try {
            DAOFactory.getInviteDAO( ).removeInvite( invite.getId( ) );
        }
        catch ( BackendException exc ) {
            Log.e( getClass( ).getName( ), "while deleting invite", exc );
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder( context )
                .setSmallIcon( R.drawable.common_plus_signin_btn_icon_dark )
                .setContentTitle( context.getString( R.string.invite_notification_title ) )
                .setContentText(
                        String.format(
                                context.getString( R.string.invite_notification_text ),
                                invite.getInviter( ).getUserName( )
                        )
                )
                .setAutoCancel( true );  // auto cancel = remove when tapped

        Uri intentUri = Uri.fromParts(
                "roadbuddy", MainActivity.INTENT_JOIN_TRIP, Integer.toString( invite.getTrip( ).getId( ) )
        );
        Intent intent = new Intent( MainActivity.INTENT_JOIN_TRIP, intentUri, context, MainActivity.class );
        intent.putExtra( MainActivity.JOIN_TRIP_INVITER_KEY, invite.getInviter( ).getUserName( ) );

        TaskStackBuilder stackBuilder = TaskStackBuilder.create( context );
        stackBuilder.addParentStack( MainActivity.class );
        stackBuilder.addNextIntent( intent );

        PendingIntent pIntent = stackBuilder.getPendingIntent(
                0, PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent( pIntent );

        NotificationManager manager = ( NotificationManager ) context.getSystemService(
                Context.NOTIFICATION_SERVICE
        );
        manager.notify( 12, builder.build( ) );
    }
}
