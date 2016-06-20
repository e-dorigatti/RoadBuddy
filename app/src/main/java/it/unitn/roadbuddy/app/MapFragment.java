package it.unitn.roadbuddy.app;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.SyncStateContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.CommentPOI;
import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.User;

import java.util.*;


public class MapFragment extends Fragment implements OnMapReadyCallback {

    MainActivity mainActivity;
    TripsFragment mTFactivity;
    FloatingActionMenu floatingActionMenu;
    ViewContainer mainLayout;
    com.sothree.slidinguppanel.SlidingUpPanelLayout slidingLayout;
    ViewContainer sliderLayout;

    Map<String, Drawable> shownDrawablesByMapId = new HashMap<>();
    Map<Integer, Drawable> shownDrawablesByModel = new HashMap<>();

    GoogleMap googleMap;
    double latitude;
    double longitude;
    LocationManager locationManager;
    Location location;
    LatLng latLng;
    Location myLocation;
    NFA nfa;
    Drawable selectedDrawable;
    User currentUser;

    FloatingActionButton button_viaggi;
    FloatingActionButton button_impost;

    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );

    NFAState initialState;

    //While selecting a Trip from recycler view we save a temporary path id to select it with
    //setSelectedDrawable(showDrawablesByModel(temp_id)
    Integer temp_id;

    public MapFragment() {
        // Required empty public constructor
    }

    public User getCurrentUser( ) {
        return mainActivity.currentUser;
    }

    public int getCurrentUserId( ) {
        return mainActivity.currentUserId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mainActivity = (MainActivity) getActivity();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int user_id = pref.getInt(SettingsFragment.KEY_PREF_USER_ID, -1);

        Log.v( "MY_STATE_LOG", "map fragment creato" );

        taskManager.startRunningTask(new GetCurrentUserAsync(), true, user_id);

        if (mainActivity.intent != null && mainActivity.intent.getAction() != null &&
                mainActivity.intent.getAction().equals(MainActivity.INTENT_JOIN_TRIP)) {

            int tripId = Integer.parseInt(mainActivity.intent.getData().getFragment());
            String inviter = mainActivity.intent.getExtras().getString(MainActivity.JOIN_TRIP_INVITER_KEY);
            initialState = new NavigationState(tripId, inviter);

            // set the intent to null to say it has been consumed
            mainActivity.intent = null;
        }
        /*else if( mainActivity.intent != null &&
                mainActivity.intent.getAction( ).equals( TripsFragment.INTENT_SELECTED_TRIP ) ){

                temp_id = mainActivity.intent.getExtras( ).getInt(TripsFragment.INTENT_SELECTED_TRIP);
                initialState = new RestState( );
        }*/
        else initialState = new RestState();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated( view, savedInstanceState);

        floatingActionMenu = (FloatingActionMenu) view.findViewById(R.id.fab);
        mainLayout = new ViewContainer(
                getLayoutInflater(savedInstanceState), getFragmentManager(),
                (FrameLayout) view.findViewById(R.id.button_container)
        );

        sliderLayout = new ViewContainer(
                getLayoutInflater(savedInstanceState), getFragmentManager(),
                (FrameLayout) view.findViewById(R.id.sliderLayout)
        );
        slidingLayout = (com.sothree.slidinguppanel.SlidingUpPanelLayout) view.findViewById(R.id.sliding_layout);
        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        SupportMapFragment mapFragment = ( SupportMapFragment ) getChildFragmentManager( ).findFragmentById( R.id.map );
        mapFragment.getMapAsync( this );
    }


    @Override
    public void onPause( ) {
        taskManager.stopAllRunningTasks( );
        if ( nfa != null )
            nfa.Pause( );

        super.onPause( );
    }

    @Override
    public void onResume( ) {
        slidingLayout.setPanelState( SlidingUpPanelLayout.PanelState.HIDDEN );
        if ( nfa != null )
            nfa.Resume( );
        super.onResume( );
    }

    @Override
    public void onStart( ) {
        super.onStart( );
    }

    @Override
    public void onDestroy( ) {
        mainActivity.mAdapter.currentMF = null;
        Log.v( "MY_STATE_LOG", "map fragment distrutto" );
        super.onDestroy( );
    }

    @Override
    public void onMapReady( GoogleMap map ) {

        googleMap = map;
        nfa = new NFA( this, initialState );
        latitude = 46.00;
        longitude = 21.00;

        if ( ActivityCompat.checkSelfPermission( getActivity( ), Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker").snippet("Snippet"));

            //Enable user's location and the button to adjust map zoom on it
            googleMap.setMyLocationEnabled( true );
            googleMap.getUiSettings( ).setMyLocationButtonEnabled( true );

            //Get Location Manager object for System Service LOCATION_SERVICE
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

            //Create a new Criteria to retrieve provider
            Criteria criteria = new Criteria();

            //Get the name of the best provider
            String provider = locationManager.getBestProvider(criteria, true);

            //Get current user location
            myLocation = locationManager.getLastKnownLocation(provider);

            //Set map type
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            if(myLocation != null) {
                //Get latitude
                latitude = myLocation.getLatitude();
                //Get longitude
                longitude = myLocation.getLongitude();
            }

            //Create a LatLng object for the current user's location
            latLng = new LatLng(latitude, longitude);

            //Show current location ong GMap
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

            //Zoom on the current user's location
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(8));
            googleMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("You are here!").snippet("Consider yourself located"));
        }
    }

    // draws a drawable and adds it to the index
    void addDrawable( Drawable drawable ) {
        String mapId = drawable.DrawToMap( getContext( ), googleMap );
        int modelId = drawable.getModelId( );

        shownDrawablesByModel.put( modelId, drawable );
        shownDrawablesByMapId.put( mapId, drawable );
    }

    // removes a drawable from the map and from the index
    void removeDrawable( Drawable drawable ) {
        drawable.RemoveFromMap( getContext( ) );

        shownDrawablesByModel.remove( drawable.getModelId( ) );
        shownDrawablesByMapId.remove( drawable.getMapId( ) );
    }

    public void RefreshMapContent( ) {
        // show rotating animation
        Animation animRotate = AnimationUtils.loadAnimation( getContext( ), R.anim.rotate );
        floatingActionMenu = ( FloatingActionMenu ) getView( ).findViewById( R.id.fab );
        if ( floatingActionMenu != null && floatingActionMenu.getAnimation( ) == null )
            if ( floatingActionMenu != null )
                floatingActionMenu.getMenuIconView( ).startAnimation( animRotate );

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
            slidingLayout.setPanelState( SlidingUpPanelLayout.PanelState.COLLAPSED );
        }
        else {
            sliderLayout.setFragment( null );
            slidingLayout.setPanelState( SlidingUpPanelLayout.PanelState.HIDDEN );
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
            addDrawable( selectedDrawable );
        }

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

            Drawable drawable = values[ 0 ];

            // FIXME [ed] shownDrawablesByModel is always empty!!!
            Drawable oldDrawable = shownDrawablesByModel.get( drawable.getModelId( ) );
            if ( oldDrawable != null )
                removeDrawable( oldDrawable );

            addDrawable( drawable );
            missingDrawables.remove( drawable.getModelId( ) );

            if ( drawable.equals( selectedDrawable ) ) {
                // they represent the same database object but are actually
                // different references so replace them but keep it selected
                selectedDrawable = drawable;
                drawable.setSelected( context, googleMap, true );
            }
           /* else if(drawable.equals(shownDrawablesByModel.get(temp_id))){

                Drawable d = shownDrawablesByModel.get(temp_id);
                setSelectedDrawable(d);
                temp_id = null;
            }*/
            else drawable.setSelected( context, googleMap, false );
        }

        @Override
        protected void onPostExecute( Boolean success ) {
            if ( success ) {
                for ( Integer drawable : missingDrawables ) {
                    removeDrawable( shownDrawablesByModel.get( drawable ) );
                }
            }
            else {
                showToast( R.string.generic_backend_error );
            }

            if ( floatingActionMenu != null )
                floatingActionMenu.getMenuIconView( ).clearAnimation( );

            super.onPostExecute( success );
        }

    }

    class GetCurrentUserAsync extends CancellableAsyncTask<Integer, Integer, User> {

        String exceptionMessage;

        public GetCurrentUserAsync( ) {
            super( taskManager );
        }

        @Override
        protected User doInBackground( Integer... userID ) {
            try {
                return DAOFactory.getUserDAO( ).getUser( userID[ 0 ] );
            }
            catch ( BackendException exc ) {
                exceptionMessage = exc.getMessage( );
                Log.e( getClass( ).getName( ), "while retrieving current user", exc );
                return null;
            }
        }

        @Override
        protected void onPostExecute( User user ) {
            if ( user != null ) {
                currentUser = user;
            }
            else if ( exceptionMessage != null ) {
                showToast( exceptionMessage );
            }
            else {
                showToast( R.string.generic_backend_error );
            }
        }
    }
    public void setZoomOnTrip(Path path){
        if ( ActivityCompat.checkSelfPermission( getActivity( ), Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {

            //Create a LatLng object for the current path's location
            LatLng latLng = (LatLng)path.getLegs().get(0).get(0);

            //Show current location ong GMap
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

            //Zoom on the current path's location
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(10));
        }

    }
    public void showTrip(Path path){
        setZoomOnTrip(path);
      //  Drawable d = shownDrawablesByModel.get(path.getId());
        Drawable d = new DrawablePath(path);

        if(d != null) {
            this.addDrawable(d);
            this.setSelectedDrawable(d);
        }
       // setSelectedDrawable(shownDrawablesByModel.get(path.getId()));
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