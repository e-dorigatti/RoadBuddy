package it.unitn.roadbuddy.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;

import java.util.ArrayList;
import java.util.List;

import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Path;


public class TripsFragment extends Fragment
        implements SearchView.OnQueryTextListener, SearchView.OnSuggestionListener {

    public static final String
            INTENT_SELECTED_TRIP = "select-trip",
            PATHS_LIST_KEY = "path-list",
            LAST_QUERY = "last-query";

    MainActivity mPActivity;
    ViewPager mPager;
    PagerAdapter mPagerAdapter;
    CancellableAsyncTaskManager taskManager;
    EmptyRecyclerView mRecyclerView;
    TripsAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;
    View emptyView;
    View tripsView;
    Location myLocation;
    GeoApiContext geoContext;
    Object lastQuery;
    SearchHintRunnable searchHintRunnable;
    SimpleCursorAdapter searchHintsAdapter;

    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private SearchView searchView;
    private List<Path> resList;

    public TripsFragment( ) {
        // Required empty public constructor
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        this.mPActivity = ( MainActivity ) getActivity( );
        super.onCreate( savedInstanceState );

        geoContext = new GeoApiContext( ).setApiKey( BuildConfig.APIKEY );

        // ShakeDetector initialization
        mSensorManager = ( SensorManager ) getActivity( ).getSystemService( Context.SENSOR_SERVICE );
        mAccelerometer = mSensorManager
                .getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
        mShakeDetector = new ShakeDetector( );
        mShakeDetector.setOnShakeListener( new ShakeDetector.OnShakeListener( ) {

            @Override
            public void onShake( int count ) {
                handleShakeEvent( );
            }
        } );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        // Inflate the layout for this fragment
        tripsView = inflater.inflate( R.layout.fragment_trips, container, false );

        searchHintsAdapter = new SimpleCursorAdapter(
                getContext( ), R.layout.layout_path_search_hint,
                null, SearchHintRunnable.COLUMNS,
                new int[] { R.id.txtLocationName }, 0
        );

        searchView = ( SearchView ) tripsView.findViewById( R.id.action_search );
        searchView.setOnQueryTextListener( this );
        searchView.setSuggestionsAdapter( searchHintsAdapter );
        searchView.setOnSuggestionListener( this );

        return tripsView;
    }

    @Override
    public void onViewCreated( View view, @Nullable Bundle savedInstanceState ) {

        super.onViewCreated( view, savedInstanceState );
        //Setting the recycler view
        this.mPager = ( ViewPager ) getActivity( ).findViewById( R.id.pager );
        this.mRecyclerView = ( EmptyRecyclerView ) view.findViewById( R.id.recycler_view );
        this.emptyView = view.findViewById( R.id.empty_view );
        mRecyclerView.setEmptyView( emptyView );
        this.mPagerAdapter = ( PagerAdapter ) mPager.getAdapter( );

        // use a linear layout manager
        this.mLayoutManager = new LinearLayoutManager( getContext( ) );
        mRecyclerView.setLayoutManager( mLayoutManager );

        double latitude = 46.00;
        double longitude = 11.00;

        this.taskManager = new CancellableAsyncTaskManager( );
        if ( ActivityCompat.checkSelfPermission( getActivity( ), Manifest.permission.ACCESS_FINE_LOCATION )
                == PackageManager.PERMISSION_GRANTED ) {

            LocationManager locationManager = ( LocationManager ) getActivity( ).getSystemService( Context.LOCATION_SERVICE );

            Criteria criteria = new Criteria( );
            String provider = locationManager.getBestProvider( criteria, true );
            myLocation = locationManager.getLastKnownLocation( provider );

            if ( myLocation != null ) {
                latitude = myLocation.getLatitude( );
                longitude = myLocation.getLongitude( );
            }
        }

        lastQuery = new LatLng( latitude, longitude );
        taskManager.startRunningTask( new getTrips( getContext( ) ), true, lastQuery );

        if ( savedInstanceState != null ) {
            resList = savedInstanceState.getParcelableArrayList( PATHS_LIST_KEY );
            mAdapter = new TripsAdapter( resList );
            mRecyclerView.setAdapter( mAdapter );
        }
    }

    @Override
    public void onPause( ) {
        // Add the following line to unregister the Sensor Manager onPause
        mSensorManager.unregisterListener( mShakeDetector );
        taskManager.stopAllRunningTasks( );

        super.onPause( );
    }

    @Override
    public void onAttach( Context context ) {
        super.onAttach( context );
    }

    @Override
    public void onDetach( ) {
        super.onDetach( );
    }

    @Override
    public void onResume( ) {
        super.onResume( );

        taskManager.startRunningTask( new getTrips( getContext( ) ), true, lastQuery );
        mSensorManager.registerListener( mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI );
    }

    public void handleShakeEvent( ) {
        showToast( "Trip list updating.." );
        taskManager.startRunningTask( new getTrips( getContext( ) ), true, lastQuery );
    }

    public void showToast( String text ) {
        Toast.makeText( getActivity( ).getApplicationContext( ), text, Toast.LENGTH_SHORT ).show( );
    }

    public void showToast( int textId ) {
        String msg = getString( textId );
        showToast( msg );
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );

        ArrayList<Path> paths = new ArrayList<>( );
        if ( resList != null ) {
            for ( Path p : resList )
                paths.add( p );
        }
        outState.putParcelableArrayList( PATHS_LIST_KEY, paths );
    }

    @Override
    public boolean onQueryTextChange( String query ) {
       /* lastQuery = query;
        if ( mPActivity.backgroundTasksHandler == null )
            return false;

        if ( searchHintRunnable != null ) {
            mPActivity.backgroundTasksHandler.removeCallbacks( searchHintRunnable );
        }

        searchHintRunnable = new SearchHintRunnable(
                geoContext, query, searchHintsAdapter, mPActivity
        );
        mPActivity.backgroundTasksHandler.postDelayed( searchHintRunnable, 1000 );
        */
        final List<Path> filteredPathList = filter( resList, query );
        if ( mAdapter != null ) {
            mAdapter.animateTo( filteredPathList );
            mRecyclerView.scrollToPosition( 0 );
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit( String query ) {
       /* lastQuery = query;
        taskManager.startRunningTask( new getTrips( getContext( ) ), true, lastQuery );*/
        final List<Path> filteredPathList = filter( resList, query );
        mAdapter.animateTo( filteredPathList );
        mRecyclerView.scrollToPosition( 0 );
        return true;

    }

    private List<Path> filter(List<Path> paths, String query) {
        query = query.toLowerCase();

        final List<Path> filteredModelList = new ArrayList<>();
        if(paths != null) {
            for (Path path : paths) {
                final String text = path.getDescription().toLowerCase();
                if (text.contains(query)) {
                    filteredModelList.add(path);
                }
            }
        }
        return filteredModelList;
    }


    @Override
    public boolean onSuggestionSelect( int position ) {
        return acceptSearchHint( position );
    }

    @Override
    public boolean onSuggestionClick( int position ) {
        return acceptSearchHint( position );
    }

    boolean acceptSearchHint( int position ) {
        Cursor cursor = ( Cursor ) searchView.getSuggestionsAdapter( ).getItem( position );
        lastQuery = new LatLng(
                cursor.getDouble( cursor.getColumnIndex( SearchHintRunnable.COLUMN_LATITUDE ) ),
                cursor.getDouble( cursor.getColumnIndex( SearchHintRunnable.COLUMN_LONGITUDE ) )
        );
        taskManager.startRunningTask( new getTrips( getContext( ) ), true, lastQuery );
        return false;
    }

    public interface ClickListener {
        void onClick( View view, int position );

        void onLongClick( View view, int position );
    }

    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private TripsFragment.ClickListener clickListener;

        public RecyclerTouchListener( Context context, final RecyclerView recyclerView, final TripsFragment.ClickListener clickListener ) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector( context, new GestureDetector.SimpleOnGestureListener( ) {
                @Override
                public boolean onSingleTapUp( MotionEvent e ) {
                    return true;
                }

                @Override
                public void onLongPress( MotionEvent e ) {
                    View child = recyclerView.findChildViewUnder( e.getX( ), e.getY( ) );
                    if ( child != null && clickListener != null ) {
                        clickListener.onLongClick( child, recyclerView.getChildPosition( child ) );
                    }
                }
            } );
        }

        @Override
        public boolean onInterceptTouchEvent( RecyclerView rv, MotionEvent e ) {

            View child = rv.findChildViewUnder( e.getX( ), e.getY( ) );
            if ( child != null && clickListener != null && gestureDetector.onTouchEvent( e ) ) {
                clickListener.onClick( child, rv.getChildPosition( child ) );
            }
            return false;
        }

        @Override
        public void onTouchEvent( RecyclerView rv, MotionEvent e ) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent( boolean disallowIntercept ) {

        }
    }

    class getTrips extends CancellableAsyncTask<Object, Integer, List<Path>> {

        String exceptionMessage;
        Context context;

        public getTrips( Context context ) {
            super( taskManager );
            this.context = context;
        }

        @Override
        protected List<Path> doInBackground( Object... pos ) {
            LatLng center;
            if ( pos[ 0 ] instanceof LatLng ) {
                center = ( LatLng ) pos[ 0 ];
            }
            else if ( pos[ 0 ] instanceof String ) {
                try {
                    GeocodingResult[] places = GeocodingApi.geocode(
                            geoContext, ( String ) pos[ 0 ]
                    ).await( );

                    center = new LatLng(
                            places[ 0 ].geometry.location.lat,
                            places[ 0 ].geometry.location.lng
                    );
                }
                catch ( Exception exc ) {
                    Log.e( getClass( ).getName( ), "while geocoding address", exc );
                    return null;
                }
            }
            else return null;

            try {
                return DAOFactory.getPathDAO( ).getPathsFromPosition( context, center, 50 * 1000 );
            }
            catch ( BackendException e ) {
                Log.e( getClass( ).getName( ), "while getting trips from position", e );
                exceptionMessage = e.getMessage( );
                return null;
            }
        }

        @Override
        protected void onPostExecute( List<Path> res ) {

            //sending data to the recycler view
            resList = res;
            if ( res == null )
                return;

            mAdapter = new TripsAdapter( resList );
            mRecyclerView.setAdapter( mAdapter );

            mRecyclerView.addOnItemTouchListener( new RecyclerTouchListener( getContext( ), mRecyclerView, new ClickListener( ) {
                @Override
                public void onClick( View view, int position ) {

                    //sending data from the recycler view to the sliderLayout
                    Path path = mAdapter.getPath( position );
                    mPActivity.showChoosenPath( path );
                    /*Intent intent = new Intent( getContext(), MainActivity.class);
                    intent.setAction(INTENT_SELECTED_TRIP);
                    Bundle savedInstanceState;
                    //intent.putExtra(INTENT_SELECTED_TRIP, path);
                    intent.putExtra(INTENT_SELECTED_TRIP, path.getId());
                    startActivity(intent);*/


                }

                @Override
                public void onLongClick( View view, int position ) {
                    showToast( "Shake to update list" );
                }
            } ) );
            super.onPostExecute( res );
        }
    }
}

class SearchHintRunnable implements Runnable {

    public static final String
            COLUMN_ID = "_id",
            COLUMN_NAME = "name",
            COLUMN_LATITUDE = "latitude",
            COLUMN_LONGITUDE = "longitude";

    public static final String[] COLUMNS = new String[] {
            COLUMN_NAME, COLUMN_LATITUDE, COLUMN_LONGITUDE, COLUMN_ID
    };

    String text;
    CursorAdapter destAdapter;
    GeoApiContext geoContext;
    Activity activity;

    public SearchHintRunnable( GeoApiContext geoContext,
                               String text,
                               CursorAdapter destAdapter,
                               Activity activity ) {
        this.text = text;
        this.destAdapter = destAdapter;
        this.geoContext = geoContext;
        this.activity = activity;
    }

    @Override
    public void run( ) {
        GeocodingResult[] places;

        try {
            places = GeocodingApi.geocode(
                    geoContext, text
            ).await( );
        }
        catch ( Exception exc ) {
            Log.e( getClass( ).getName( ), "while retrieving search hints", exc );
            return;
        }

        final MatrixCursor cursor = new MatrixCursor( COLUMNS );
        for ( int i = 0; i < places.length; i++ ) {
            GeocodingResult res = places[ i ];

            cursor.addRow( new Object[] {
                    res.formattedAddress,
                    res.geometry.location.lat,
                    res.geometry.location.lng,
                    Integer.toString( i )
            } );
        }

        activity.runOnUiThread( new Runnable( ) {
            @Override
            public void run( ) {
                destAdapter.changeCursor( cursor );
            }
        } );
    }
}
