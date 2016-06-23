package it.unitn.roadbuddy.app;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.gms.maps.model.LatLng;
import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NavigationInfoFragment extends SliderContentFragment implements AdapterView.OnItemClickListener {

    ParticipantInteractionListener listener;
    ListView lstBuddies;
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

        lstBuddies = ( ListView ) view.findViewById( R.id.lstBuddies );
        if ( lstBuddies != null ) {
            adapter = new DynamicViewArrayAdapter( getContext( ), adaptedBuddies );
            lstBuddies.setAdapter( adapter );
            lstBuddies.setOnItemClickListener( this );
        }

        FrameLayout frmPathInfo = ( FrameLayout ) view.findViewById( R.id.frmPathInfo );
        if ( tripPath != null && frmPathInfo != null ) {
            FragmentTransaction transaction = getFragmentManager( ).beginTransaction( );
            transaction.add( R.id.frmPathInfo, tripPath.getInfoFragment( ) );
            transaction.commit( );
        }

        return view;
    }

    public void setBuddies( List<User> buddies ) {
        adaptedBuddies.clear( );
        for ( User u : buddies ) {
            if ( u.getId( ) == currentUserId )
                currentUserPosition = u.getLastPosition( );

            adaptedBuddies.add( new AdaptedUser( u ) );
        }
        Collections.sort( adaptedBuddies, new AdapterUserComparator( ) );
        adapter.notifyDataSetChanged( );
    }

    @Override
    public void onItemClick( AdapterView<?> adapterView, View view, int position, long id ) {
        if ( listener != null ) {
            AdaptedUser selected = ( AdaptedUser ) adaptedBuddies.get( position );
            listener.onParticipantSelected( selected.getUser( ) );
            setSelectedUser( selected.getUser( ) );
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
    }

    class AdaptedUser implements DynamicViewArrayAdapter.Listable {

        Double distanceFromUser = null;
        User user;
        boolean selected;

        public AdaptedUser( User user ) {
            this.user = user;
            this.selected = false;
        }

        public void setSelected( boolean selected ) {
            this.selected = selected;
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            TextView txt = new TextView( getContext( ) );
            txt.setTextColor( Color.WHITE );
            txt.setCompoundDrawablesWithIntrinsicBounds( R.mipmap.ic_person_outline_white_24dp, 0, 0, 0 );
            txt.setCompoundDrawablePadding( 56 );
            float density = getContext( ).getResources( ).getDisplayMetrics( ).density;
            int h = ( int ) ( 68 * density );
            txt.setHeight( h );
            txt.setWidth( ViewGroup.LayoutParams.MATCH_PARENT );
            txt.setPadding( 16, 8, 16, 0 );
            txt.setGravity( Gravity.CENTER_VERTICAL );

            if ( selected )
                txt.setTextColor( Color.RED );

            if ( user.getId( ) == currentUserId ) {
                String fmt = getString( R.string.navigation_info_you );
                txt.setText( String.format( fmt, user.getUserName( ) ) );
            }
            else {
                Double dist = getDistanceFromCurrentUser( );
                String fmt = getString( R.string.navigation_info_buddy );
                txt.setText( String.format(
                        fmt, user.getUserName( ),
                        dist != null ? Path.formatDistance( dist.intValue( ) ) : "???"
                ) );
            }

            return txt;
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
