package it.unitn.roadbuddy.app;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.CommentPOI;
import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MapFragment extends Fragment implements OnMapReadyCallback {

    MainActivity mPActivity;
    FloatingActionMenu floatingActionMenu;
    ViewContainer mainLayout;
    com.sothree.slidinguppanel.SlidingUpPanelLayout slidingLayout;
    ViewContainer sliderLayout;

    GoogleMap googleMap;
    NFA nfa;
    Map<String, Drawable> shownDrawables = new HashMap<>();
    Drawable selectedDrawable;
    User currentUser;

    FloatingActionButton button_viaggi;
    FloatingActionButton button_impost;

    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager();

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mPActivity = (MainActivity) getActivity();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        long user_id = pref.getLong(SettingsFragment.KEY_PREF_USER_ID, -1);

        taskManager.startRunningTask(new GetCurrentUserAsync(), true, user_id);
            }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        button_viaggi = (FloatingActionButton) view.findViewById(R.id.button_map_viaggi);
        button_impost = (FloatingActionButton) view.findViewById(R.id.button_map_impost);


        button_viaggi.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mPActivity.mPager.setCurrentItem(1);
                return false;
            }
        });
        button_impost.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mPActivity.mPager.setCurrentItem(2);
                return false;
            }
        });

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


        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onPause() {
        taskManager.stopRunningTasksOfType(RefreshMapAsync.class);
        if (nfa != null)
            nfa.Pause();

        super.onPause();
    }

    @Override
    public void onResume() {
        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        if (nfa != null)
            nfa.Resume();
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onMapReady(GoogleMap map) {

        googleMap = map;
        nfa = new NFA(this, new RestState());

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings( ).setMyLocationButtonEnabled( true );
        }
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
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
        else{
            sliderLayout.setFragment( null );
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        }
    }

    class RefreshMapAsync extends CancellableAsyncTask<LatLngBounds, Integer, List<Drawable>> {

        String exceptionMessage;
        Context context;

        public RefreshMapAsync( Context context ) {
            super( taskManager );
            this.context = context;
        }

        @Override
        protected List<Drawable> doInBackground( LatLngBounds... bounds ) {
            /**
             * Sleep a while to let the user adjust the viewport
             *
             * Usually the view is moved using many small changes in
             * rapid succession. A task is started for every such move
             * and cancelled when the view is moved again
             */
            SystemClock.sleep( 2500 );
            if ( isCancelled( ) ) return null;

            try {
                List<Drawable> results = new ArrayList<>( );

                List<Path> paths = DAOFactory.getPathDAO( ).getPathsInside(
                        context, bounds[ 0 ]
                );

                List<CommentPOI> commentPOIs =
                        DAOFactory.getPoiDAOFactory( )
                                .getCommentPoiDAO( )
                                .getCommentPOIsInside(
                                        context,
                                        bounds[ 0 ]
                                );

                for ( Path p : paths )
                    results.add( new DrawablePath( p ) );

                for ( CommentPOI p : commentPOIs )
                    results.add( new DrawableCommentPOI( p ) );

                return results;
            }
            catch ( BackendException e ) {
                Log.e( getClass( ).getName( ), "while refreshing map", e );
                exceptionMessage = e.getMessage( );
                return null;
            }
        }

        @Override
        protected void onPostExecute( List<Drawable> drawables ) {
            if ( drawables != null ) {
                for ( Map.Entry<String, Drawable> entry : shownDrawables.entrySet( ) ) {
                    entry.getValue( ).RemoveFromMap( context );
                }

                shownDrawables.clear( );
                for ( Drawable d : drawables ) {
                    String displayed = d.DrawToMap( context, googleMap );
                    shownDrawables.put( displayed, d );

                    if ( d.equals( selectedDrawable ) ) {
                        // they represent the same database object but are actually different references
                        selectedDrawable = d;
                        d.setSelected( context, googleMap, true );
                    }
                    else d.setSelected( context, googleMap, false );
                }
            }
            else if ( exceptionMessage != null ) {
                showToast( exceptionMessage );
            }
            else {
                showToast( R.string.generic_backend_error );
            }

            if ( floatingActionMenu != null )
                floatingActionMenu.getMenuIconView( ).clearAnimation( );
        }
    }

    class GetCurrentUserAsync extends CancellableAsyncTask<Long, Integer, User> {

        String exceptionMessage;

        public GetCurrentUserAsync( ) {
            super( taskManager );
        }

        @Override
        protected User doInBackground( Long... userID ) {
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