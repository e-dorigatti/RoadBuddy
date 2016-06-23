package it.unitn.roadbuddy.app;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.CommentPOI;
import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.User;

import java.util.*;


public class MapFragment extends Fragment implements OnMapReadyCallback {

    final static String DRAWABLES_LIST_KEY = "drawables-by-model-id",
            DRAWABLE_KEY_FORMAT = "drawable-%d",
            SELECTED_DRAWABLE_KEY = "selected-drawable",
            CAMERA_LOCATION_KEY = "camera-location";

    MainActivity mainActivity;
    FloatingActionMenu floatingActionMenu;
    ViewContainer mainLayout;
    ViewContainer sliderLayout;
    Map<String, Drawable> shownDrawablesByMapId = new HashMap<>( );
    Map<Integer, Drawable> shownDrawablesByModel = new HashMap<>( );
    GoogleMap googleMap;
    NFA nfa;
    Drawable selectedDrawable;
    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );
    NFAState initialState;
    Bundle savedInstanceState;
    SetSliderStatusRunnable setSliderStatus;
    private SlidingUpPanelLayout slidingLayout;

    public MapFragment( ) {
    }

    public User getCurrentUser( ) {
        return mainActivity.currentUser;
    }

    public int getCurrentUserId( ) {
        return mainActivity.currentUserId;
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        this.mainActivity = ( MainActivity ) getActivity( );
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( getActivity( ) );
        int user_id = pref.getInt( SettingsFragment.KEY_PREF_USER_ID, -1 );

        Log.v( "MY_STATE_LOG", "map fragment creato" );

        if ( savedInstanceState == null ) {
            if ( mainActivity.intent != null && mainActivity.intent.getAction( ) != null &&
                    mainActivity.intent.getAction( ).equals( MainActivity.INTENT_JOIN_TRIP ) ) {

                int tripId = Integer.parseInt( mainActivity.intent.getData( ).getFragment( ) );
                String inviter = mainActivity.intent.getExtras( ).getString( MainActivity.JOIN_TRIP_INVITER_KEY );
                initialState = new NavigationState( tripId, inviter );

                // set the intent to null to say it has been consumed
                mainActivity.intent = null;
            }
            else initialState = new RestState( );
        }
        else this.savedInstanceState = savedInstanceState;
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        return inflater.inflate( R.layout.fragment_map, container, false );
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated( view, savedInstanceState );

        this.savedInstanceState = savedInstanceState;
        floatingActionMenu = ( FloatingActionMenu ) view.findViewById( R.id.fab );
        mainLayout = new ViewContainer(
                getLayoutInflater( savedInstanceState ), getFragmentManager( ),
                ( FrameLayout ) view.findViewById( R.id.button_container )
        );

        sliderLayout = new ViewContainer(
                getLayoutInflater( savedInstanceState ), getFragmentManager( ),
                ( FrameLayout ) view.findViewById( R.id.sliderLayout )
        );
        slidingLayout = ( com.sothree.slidinguppanel.SlidingUpPanelLayout ) view.findViewById( R.id.sliding_layout );
        slidingLayout.setPanelState( SlidingUpPanelLayout.PanelState.HIDDEN );

        SupportMapFragment mapFragment = ( SupportMapFragment ) getChildFragmentManager( ).findFragmentById( R.id.map );
        mapFragment.getMapAsync( this );
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        if ( nfa != null )
            nfa.onSaveInstanceState( outState );

        ArrayList<Parcelable> drawables = new ArrayList<>( );
        for ( Drawable d : shownDrawablesByModel.values( ) )
            drawables.add( d );
        outState.putParcelableArrayList( DRAWABLES_LIST_KEY, drawables );

        if ( selectedDrawable != null ) {
            outState.putInt( SELECTED_DRAWABLE_KEY, selectedDrawable.getModelId( ) );
        }

        outState.putParcelable( CAMERA_LOCATION_KEY, googleMap.getCameraPosition( ) );
    }

    @Override
    public void onStop( ) {
        taskManager.stopAllRunningTasks( );
        if ( nfa != null )
            nfa.Pause( );
        super.onStop( );
    }

    @Override
    public void onStart( ) {
        setSLiderStatus( SlidingUpPanelLayout.PanelState.HIDDEN );
        if ( nfa != null )
            nfa.Resume( savedInstanceState );
        super.onStart( );
    }

    @Override
    public void onDestroy( ) {
        mainActivity.mAdapter.mapFragment = null;
        Log.v( "MY_STATE_LOG", "map fragment distrutto" );
        super.onDestroy( );
    }

    @Override
    public void onMapReady( GoogleMap map ) {
        googleMap = map;
        googleMap.setMapType( GoogleMap.MAP_TYPE_NORMAL );

        CameraPosition savedCameraPosition = null;
        if ( savedInstanceState != null ) {
            map.clear( );

            ArrayList<Drawable> drawables = savedInstanceState.getParcelableArrayList( DRAWABLES_LIST_KEY );
            for ( Drawable d : drawables ) {
                /**
                 * for some reason polylines in drawable paths are preserved
                 * even when marked as transient and not written in the parcel
                 * causing funny bugs as we've just cleared the map, but the
                 * polyline still thinks she's visible
                 */

                d.RemoveFromMap( getContext( ) );
                addDrawable( d );
            }

            int selectedDrawableId = savedInstanceState.getInt( SELECTED_DRAWABLE_KEY, -1 );
            if ( selectedDrawableId >= 0 ) {
                selectedDrawable = shownDrawablesByModel.get( selectedDrawableId );
                setSelectedDrawable( selectedDrawable );
            }

            savedCameraPosition = savedInstanceState.getParcelable( CAMERA_LOCATION_KEY );
        }

        if ( ActivityCompat.checkSelfPermission( getActivity( ), Manifest.permission.ACCESS_FINE_LOCATION ) ==
                PackageManager.PERMISSION_GRANTED ) {

            //Enable user's location and the button to adjust map zoom on it
            googleMap.setMyLocationEnabled( true );
            googleMap.getUiSettings( ).setMyLocationButtonEnabled( true );

            if ( savedCameraPosition != null ) {
                googleMap.moveCamera( CameraUpdateFactory.newCameraPosition( savedCameraPosition ) );
            }
            else {
                LocationManager locationManager = ( LocationManager ) getActivity( )
                        .getSystemService( Context.LOCATION_SERVICE );

                Criteria criteria = new Criteria( );
                String provider = locationManager.getBestProvider( criteria, true );

                Location myLocation = locationManager.getLastKnownLocation( provider );
                if ( myLocation != null ) {
                    LatLng myPos = new LatLng( myLocation.getLatitude( ),
                                               myLocation.getLongitude( ) );

                    googleMap.moveCamera( CameraUpdateFactory.newLatLng( myPos ) );
                    googleMap.animateCamera( CameraUpdateFactory.zoomTo( 8 ) );
                }
            }
        }

        nfa = new NFA( this, initialState, savedInstanceState );
        savedInstanceState = null;
    }

    /**
     * When issuing many status changes in a short amount of time
     * only the first one gets executed and the rest is discarded.
     * <p/>
     * We keep track of the latest such status and set it some time
     * in the future so as to allow everyone to set a status
     */
    void setSLiderStatus( SlidingUpPanelLayout.PanelState status ) {
        if ( mainActivity.backgroundTasksHandler != null ) {
            if ( setSliderStatus != null )
                mainActivity.backgroundTasksHandler.removeCallbacks( setSliderStatus );

            setSliderStatus = new SetSliderStatusRunnable(
                    slidingLayout,
                    status
            );

            mainActivity.backgroundTasksHandler.postDelayed( setSliderStatus, 500 );
        }
        else slidingLayout.setPanelState( status );
    }

    // draws a drawable and adds it to the index
    void addDrawable( Drawable drawable ) {
        if ( shownDrawablesByModel.get( drawable.getModelId( ) ) != null )
            return;

        String mapId = drawable.DrawToMap( getContext( ), googleMap );
        int modelId = drawable.getModelId( );

        shownDrawablesByModel.put( modelId, drawable );
        shownDrawablesByMapId.put( mapId, drawable );
    }

    // removes a drawable from the map and from the index
    void removeDrawable( Drawable drawable ) {
        if ( drawable == null )
            return;

        if ( drawable == selectedDrawable )
            setSelectedDrawable( null );

        drawable.RemoveFromMap( getContext( ) );

        shownDrawablesByModel.remove( drawable.getModelId( ) );
        shownDrawablesByMapId.remove( drawable.getMapId( ) );
    }

    public void RefreshMapContent( ) {
        // run async task
        LatLngBounds bounds = googleMap.getProjection( ).getVisibleRegion( ).latLngBounds;
        taskManager.startRunningTask( new RefreshMapAsync( getContext( ) ), true, bounds );
    }

    public void showToast( String text ) {
        Toast.makeText( getActivity( ).getApplicationContext( ), text, Toast.LENGTH_LONG ).show( );
    }

    public void showToast( int textId ) {
        String msg = getString( textId );
        showToast( msg );
    }

    public void setSelectedDrawable( Drawable d ) {
        if ( selectedDrawable != null )
            selectedDrawable.setSelected( getContext( ), googleMap, false );

        selectedDrawable = d;

        if ( selectedDrawable != null ) {
            selectedDrawable.setSelected( getContext( ), googleMap, true );
            sliderLayout.setFragment( selectedDrawable.getInfoFragment( ) );
            setSLiderStatus( SlidingUpPanelLayout.PanelState.COLLAPSED );
        }
        else {
            sliderLayout.setFragment( null );
            setSLiderStatus( SlidingUpPanelLayout.PanelState.HIDDEN );
        }
    }

    public void clearMap( ) {
        for ( Map.Entry<String, Drawable> entry : shownDrawablesByMapId.entrySet( ) ) {
            if ( entry.getValue( ) != selectedDrawable ) {
                entry.getValue( ).RemoveFromMap( getContext( ) );
            }
        }

        shownDrawablesByMapId.clear( );
        shownDrawablesByModel.clear( );

        if ( selectedDrawable != null ) {
            shownDrawablesByMapId.put( selectedDrawable.getMapId( ), selectedDrawable );
            shownDrawablesByModel.put( selectedDrawable.getModelId( ), selectedDrawable );
        }
    }

    public void setZoomOnTrip( Path path ) {
        if ( ActivityCompat.checkSelfPermission( getActivity( ), Manifest.permission.ACCESS_FINE_LOCATION )
                == PackageManager.PERMISSION_GRANTED ) {

            LatLng pathStart = path.getLegs( ).get( 0 ).get( 0 );
            googleMap.moveCamera( CameraUpdateFactory.newLatLng( pathStart ) );
            googleMap.animateCamera( CameraUpdateFactory.zoomTo( 10 ) );
        }

    }

    public void showTrip( Path path ) {
        setZoomOnTrip( path );

        Drawable d = shownDrawablesByModel.get( path.getId( ) );
        if ( d == null ) {
            d = new DrawablePath( path );
            addDrawable( d );
        }

        setSelectedDrawable( d );
    }

    class RefreshMapAsync extends CancellableAsyncTask<LatLngBounds, Drawable, Boolean> {

        String exceptionMessage;
        Context context;

        /**
         * drawables which were not returned by the backend will be removed from the
         * map because they are not visible anymore
         */
        Set<Integer> missingDrawables;

        public RefreshMapAsync( Context context ) {
            super( taskManager );
            this.context = context;

            // get a copy of the key set, otherwise when removing from it we
            // will remove from the map, too
            missingDrawables = new HashSet<>( shownDrawablesByModel.keySet( ) );
        }

        @Override
        protected Boolean doInBackground( LatLngBounds... bounds ) {
            /**
             * Sleep a while to let the user adjust the viewport
             *
             * Usually the view is moved using many small changes in
             * rapid succession. A task is started for every such move
             * and cancelled when the view is moved again
             */
            SystemClock.sleep( 2500 );
            if ( isCancelled( ) ) return false;

            try {

                List<Path> paths = DAOFactory.getPathDAO( ).getPathsInside(
                        context, bounds[ 0 ]
                );

                for ( Path p : paths ) {
                    publishProgress( new DrawablePath( p ) );
                }

                List<CommentPOI> commentPOIs =
                        DAOFactory.getPoiDAOFactory( )
                                  .getCommentPoiDAO( )
                                  .getCommentPOIsInside(
                                          context,
                                          bounds[ 0 ]
                                  );

                for ( CommentPOI p : commentPOIs ) {
                    publishProgress( new DrawableCommentPOI( p ) );
                }

                return true;
            }
            catch ( BackendException e ) {
                Log.e( getClass( ).getName( ), "while refreshing map", e );
                exceptionMessage = e.getMessage( );
                return false;
            }
        }

        @Override
        protected void onProgressUpdate( Drawable... values ) {
            super.onProgressUpdate( values );

            Drawable selected = selectedDrawable;
            Drawable drawable = values[ 0 ];

            Drawable oldDrawable = shownDrawablesByModel.get( drawable.getModelId( ) );
            if ( oldDrawable != null )
                removeDrawable( oldDrawable );

            addDrawable( drawable );
            if ( drawable.equals( selected ) )
                setSelectedDrawable( drawable );

            missingDrawables.remove( drawable.getModelId( ) );
        }

        @Override
        protected void onPostExecute( Boolean success ) {
            if ( success ) {
                for ( Integer drawable : missingDrawables ) {
                    removeDrawable( shownDrawablesByModel.get( drawable ) );
                }
            }

            if ( floatingActionMenu != null )
                floatingActionMenu.getMenuIconView( ).clearAnimation( );

            super.onPostExecute( success );
        }

    }

    class SetSliderStatusRunnable implements Runnable {

        SlidingUpPanelLayout panel;
        SlidingUpPanelLayout.PanelState state;

        public SetSliderStatusRunnable( SlidingUpPanelLayout panel, SlidingUpPanelLayout.PanelState state ) {
            this.panel = panel;
            this.state = state;
        }

        @Override
        public void run( ) {
            mainActivity.runOnUiThread( new Runnable( ) {
                @Override
                public void run( ) {
                    panel.setPanelState( state );
                }
            } );

            setSliderStatus = null;
        }
    }
}

class ViewContainer {

    LayoutInflater inflater;
    FragmentManager fragmentManager;
    ViewGroup container;
    View currentView;
    Fragment currentFragment;

    public ViewContainer( LayoutInflater inflater,
                          FragmentManager fragmentManager,
                          ViewGroup container ) {

        this.container = container;
        this.inflater = inflater;
        this.fragmentManager = fragmentManager;
    }

    public void removeView( ) {
        if ( currentView != null ) {
            container.removeView( currentView );
            currentView = null;
        }
        else if ( currentFragment != null ) {
            FragmentTransaction ft = fragmentManager.beginTransaction( );
            ft.remove( currentFragment );
            ft.commit( );
            currentFragment = null;
        }
    }

    public void setView( View v ) {
        removeView( );
        if ( v != null ) {
            currentView = v;

            if ( v.getParent( ) == null )
                container.addView( v );
            else Utils.Assert( v.getParent( ) == container, false );

            showParent( );
        }
        else hideParent( );
    }

    public View setView( int resId ) {
        View v = inflater.inflate( resId, container, false );
        setView( v );
        return v;
    }

    public void setFragment( Fragment f ) {
        removeView( );

        if ( f != null ) {
            currentFragment = f;

            FragmentTransaction ft = fragmentManager.beginTransaction( );
            ft.add( container.getId( ), f );
            ft.commit( );

            showParent( );
        }
        else hideParent( );
    }

    public void showParent( ) {
        //container.setVisibility( View.VISIBLE );
    }

    public void hideParent( ) {
        //container.setVisibility( View.INVISIBLE );
    }
}