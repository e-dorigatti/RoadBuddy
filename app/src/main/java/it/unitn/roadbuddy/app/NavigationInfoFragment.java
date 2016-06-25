package it.unitn.roadbuddy.app;

import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.android.gms.maps.model.LatLng;
import it.unitn.roadbuddy.app.backend.models.Notification;
import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.User;

import java.util.*;

public class NavigationInfoFragment extends SliderContentFragment
        implements AdapterView.OnItemClickListener {

    ParticipantInteractionListener listener;
    DynamicViewArrayAdapter adapter;
    List<DynamicViewArrayAdapter.Listable> adaptedBuddies = new ArrayList<>( );

    DrawablePath tripPath;
    AdaptedUser selected;

    // info about the user of this app
    int currentUserId;
    LatLng currentUserPosition;

    public static NavigationInfoFragment newInstance( int currentUserId, DrawablePath tripPath ) {
        NavigationInfoFragment f = new NavigationInfoFragment( );
        f.smallViewId = R.layout.fragment_navigation_info_large;
        f.currentUserId = currentUserId;
        f.tripPath = tripPath;
        return f;
    }

    public void setParticipantInteractionListener( ParticipantInteractionListener listener ) {
        this.listener = listener;
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {

        View view = super.onCreateView( inflater, container, savedInstanceState );

        ListView lstBuddies = ( ListView ) view.findViewById( R.id.lstBuddies );
        if ( lstBuddies != null ) {
            adapter = new DynamicViewArrayAdapter( getContext( ), adaptedBuddies );
            lstBuddies.setAdapter( adapter );
            lstBuddies.setOnItemClickListener( this );
        }

        FloatingActionButton btnSendPing = ( FloatingActionButton ) view.findViewById( R.id.btnSendPing );
        if ( btnSendPing != null ) {
            btnSendPing.setOnClickListener( new View.OnClickListener( ) {
                @Override
                public void onClick( View view ) {
                    if ( listener != null ) {
                        listener.onSendPing( );
                        Toast.makeText( getContext( ), "Sent!", Toast.LENGTH_SHORT ).show( );
                    }
                }
            } );
        }

        FrameLayout frmPathInfo = ( FrameLayout ) view.findViewById( R.id.frmPathInfo );
        if ( tripPath != null && frmPathInfo != null ) {
            FragmentTransaction transaction = getFragmentManager( ).beginTransaction( );
            transaction.add( R.id.frmPathInfo, tripPath.getInfoFragment( ) );
            transaction.commit( );

            frmPathInfo.setOnClickListener( new View.OnClickListener( ) {
                @Override
                public void onClick( View view ) {
                    if ( listener != null )
                        listener.onPathSelected( );
                }
            } );
        }

        return view;
    }

    public void setBuddies( List<User> buddies ) {
        Map<Integer, Integer> badges = new HashMap<>( );
        for ( DynamicViewArrayAdapter.Listable listable : adaptedBuddies ) {
            AdaptedUser adapted = ( AdaptedUser ) listable;
            badges.put( adapted.getUser( ).getId( ), adapted.getBadgeType( ) );
        }

        adaptedBuddies.clear( );
        for ( User u : buddies ) {
            if ( u.getId( ) == currentUserId )
                currentUserPosition = u.getLastPosition( );

            AdaptedUser adaptedUser = new AdaptedUser( u );
            adaptedBuddies.add( adaptedUser );
            Integer badge = badges.get( u.getId( ) );
            if ( badge != null ) {
                adaptedUser.setBadgeType( badge );
            }
        }
        Collections.sort( adaptedBuddies, new AdapterUserComparator( ) );
        adapter.notifyDataSetChanged( );
    }

    public void showNotifications( List<Notification> notifications ) {
        if ( notifications.size( ) == 0 )
            return;

        Map<Integer, AdaptedUser> shownUsers = new HashMap<>( );
        for ( DynamicViewArrayAdapter.Listable listable : adaptedBuddies ) {
            AdaptedUser adapted = ( AdaptedUser ) listable;
            shownUsers.put( adapted.getUser( ).getId( ), adapted );
        }

        for ( Notification not : notifications ) {
            AdaptedUser adaptedSender = shownUsers.get( not.getSender( ).getId( ) );
            if ( adaptedSender != null ) {
                adaptedSender.setBadgeType( not.getType( ) );
            }
        }
    }

    @Override
    public void onItemClick( AdapterView<?> adapterView, View view, int position, long id ) {
        if ( listener != null ) {
            AdaptedUser selected = ( AdaptedUser ) adaptedBuddies.get( position );

            listener.onParticipantSelected( selected.getUser( ) );
            setSelectedUser( selected.getUser( ) );
            selected.setBadgeType( 0 );
        }
    }

    public void setSelectedUser( User user ) {
        if ( selected != null )
            selected.setSelected( false );

        if ( user != null ) {
            for ( DynamicViewArrayAdapter.Listable a : adaptedBuddies ) {
                AdaptedUser adapted = ( AdaptedUser ) a;
                if ( adapted.getUser( ).getId( ) != user.getId( ) )
                    continue;

                selected = adapted;
                selected.setSelected( true );
            }
        }

        adapter.notifyDataSetChanged( );
    }

    public interface ParticipantInteractionListener {
        void onParticipantSelected( User participant );

        void onPathSelected( );

        void onSendPing( );
    }

    class AdaptedUser implements DynamicViewArrayAdapter.Listable {

        private static final int BLINK_DELAY = 500;

        Double distanceFromUser = null;
        User user;
        boolean selected;
        int badge = 0;

        TextView userInfoView;
        AnimationDrawable blinkAnimation;
        int avatar;

        public AdaptedUser( User user ) {
            this.user = user;
            this.selected = false;
        }

        public void setSelected( boolean selected ) {
            this.selected = selected;
        }

        public int getBadgeType( ) {
            return badge;
        }

        public void setBadgeType( int type ) {
            badge = type;
            if ( type <= 0 ) {
                if ( blinkAnimation != null )
                    blinkAnimation.stop( );
            }
            else {
                setupAnimation( );
                if ( blinkAnimation != null ) {
                    userInfoView.setCompoundDrawablesWithIntrinsicBounds(
                            blinkAnimation, null, null, null
                    );
                    blinkAnimation.start( );
                }
            }

        }

        void setupAnimation( ) {
            int drawable;
            switch ( badge ) {
                case Notification.NOTIFICATION_PING:
                    drawable = R.drawable.ic_warning_white_24dp;
                    break;

                default:
                    drawable = 0;
                    break;
            }

            if ( userInfoView != null && drawable > 0 ) {
                blinkAnimation = new AnimationDrawable( );
                blinkAnimation.addFrame( getResources( ).getDrawable( avatar ), BLINK_DELAY );
                blinkAnimation.addFrame( getResources( ).getDrawable( drawable ), BLINK_DELAY );
                blinkAnimation.setOneShot( false );
            }
            else blinkAnimation = null;
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            userInfoView = new TextView( getContext( ) );
            userInfoView.setTextColor( Color.WHITE );
            switch ( position ) {
                case 0:
                    avatar = R.mipmap.ic_avatar1;
                    break;
                case 1:
                    avatar = R.mipmap.ic_avatar2;
                    break;
                case 2:
                    avatar = R.mipmap.ic_avatar3;
                    break;
                default:
                    avatar = R.mipmap.ic_avatar4;
                    break;
            }

            userInfoView.setCompoundDrawablePadding( 56 );

            setupAnimation( );
            if ( blinkAnimation != null ) {
                userInfoView.setCompoundDrawablesWithIntrinsicBounds(
                        blinkAnimation, null, null, null
                );
                blinkAnimation.start( );
            }
            else {
                userInfoView.setCompoundDrawablesWithIntrinsicBounds( avatar, 0, 0, 0 );
            }

            float density = getContext( ).getResources( ).getDisplayMetrics( ).density;
            int height = ( int ) ( 68 * density );
            userInfoView.setLayoutParams( new TableLayout.LayoutParams( ViewGroup.LayoutParams.FILL_PARENT, height, 1 ) );
            userInfoView.setPadding( 16, 8, 16, 0 );
            userInfoView.setGravity( Gravity.CENTER_VERTICAL );

            if ( selected )
                userInfoView.setTextColor( Color.RED );

            if ( user.getId( ) == currentUserId ) {
                String fmt = getString( R.string.navigation_info_you );
                userInfoView.setText( String.format( fmt, user.getUserName( ) ) );
            }
            else {
                Double dist = getDistanceFromCurrentUser( );
                String fmt = getString( R.string.navigation_info_buddy );
                userInfoView.setText( String.format(
                        fmt, user.getUserName( ),
                        dist != null ? Path.formatDistance( dist.intValue( ) ) : "???"
                ) );
            }

            return userInfoView;
        }

        public User getUser( ) {
            return user;
        }

        public Double getDistanceFromCurrentUser( ) {
            if ( distanceFromUser == null ) {
                // https://en.wikipedia.org/wiki/Haversine_formula

                /**
                 * when a the user position changes this adapted user is thrown away
                 * and a new one is created. moreover, when sorting we should compute
                 * this value n^2 times (see the comparator), so by caching it we
                 * avoid some useless computations
                 */
                if ( user.getLastPosition( ) == null || currentUserPosition == null )
                    return null;

                double earth_radius = 6371 * 1000; // metres
                double phi1 = ( currentUserPosition.latitude * Math.PI ) / 180;
                double phi2 = ( user.getLastPosition( ).latitude * Math.PI ) / 180;

                double lambda1 = ( currentUserPosition.longitude * Math.PI ) / 180;
                double lambda2 = ( user.getLastPosition( ).longitude * Math.PI ) / 180;

                double a = Math.pow( Math.sin( ( phi2 - phi1 ) / 2 ), 2 );
                double b = Math.pow( Math.sin( ( lambda2 - lambda1 ) / 2 ), 2 );
                double c = Math.sqrt( a + b * Math.cos( phi1 ) * Math.cos( phi2 ) );

                distanceFromUser = 2 * earth_radius * Math.asin( c );
            }

            return distanceFromUser;
        }
    }

    class AdapterUserComparator implements Comparator<DynamicViewArrayAdapter.Listable> {

        @Override
        public int compare( DynamicViewArrayAdapter.Listable l1,
                            DynamicViewArrayAdapter.Listable l2 ) {

            AdaptedUser u1 = ( AdaptedUser ) l1;
            AdaptedUser u2 = ( AdaptedUser ) l2;

            Double d1 = u1.getDistanceFromCurrentUser( );
            Double d2 = u2.getDistanceFromCurrentUser( );

            // show MIA riders first
            if ( d1 == null )
                return 1;
            else if ( d2 == null )
                return -1;
            else
                return -Double.compare( d1, d2 );
        }
    }
}
