package it.unitn.roadbuddy.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Path;


public class TripsFragment extends Fragment {

    MainActivity mPActivity;
    ViewPager mPager;
    PagerAdapter mPagerAdapter;
    CancellableAsyncTaskManager taskManager;
    EmptyRecyclerView mRecyclerView;
    TripsAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;
    View emptyView;
    View tripsView;
    private SearchView searchView;
    public static final String INTENT_SELECTED_TRIP = "select-trip";




    FloatingActionButton button_map;
    FloatingActionButton button_impost;

    public TripsFragment( ) {
        // Required empty public constructor

    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        this.mPActivity = (MainActivity) getActivity();
        super.onCreate( savedInstanceState );

    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        // Inflate the layout for this fragment
        tripsView = inflater.inflate( R.layout.fragment_trips, container, false );
        return tripsView;

    }

    @Override
    public void onViewCreated( View view, @Nullable Bundle savedInstanceState ) {
        super.onViewCreated( view, savedInstanceState );


        //Setting navigation buttons
        button_map = (FloatingActionButton) getActivity().findViewById(R.id.button_trips_map);
        button_impost = (FloatingActionButton) getActivity().findViewById(R.id.button_trips_impost);


        button_map.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mPActivity.mPager.setCurrentItem(0);
                return false;
            }
        });
        button_impost.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mPActivity.mPager.setCurrentItem(2);
                return false;
            }
        });

        //prepare the SearchView
        //searchView = (SearchView) searchView.findViewById(R.id.search_bar);
        //SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);


        // Associate searchable configuration with the SearchView
      /*  SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) trips_view.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));*/


        //Setting the recycler view
        this.mPager = ( ViewPager ) getActivity( ).findViewById( R.id.pager );
        this.mRecyclerView = ( EmptyRecyclerView ) view.findViewById( R.id.recycler_view );
        this.emptyView = view.findViewById(R.id.empty_view);
        mRecyclerView.setEmptyView(emptyView);
        this.mPagerAdapter = ( PagerAdapter ) mPager.getAdapter( );

        // use a linear layout manager
        this.mLayoutManager = new LinearLayoutManager( getContext( ) );
        mRecyclerView.setLayoutManager( mLayoutManager );

        this.taskManager = new CancellableAsyncTaskManager( );
        LatLng myPos = new LatLng( 46.0829800, 11.1155410 );

        taskManager.startRunningTask( new getTrips( getContext( ) ), true, myPos );

    }

    @Override
    public void onPause( ) {
        super.onPause( );
        taskManager.stopRunningTasksOfType( getTrips.class );
    }

    @Override
    public void onAttach( Context context ) {
        super.onAttach( context );
    }

    @Override
    public void onDetach( ) {
        super.onDetach( );
    }

    public void updateList( ) {
        LatLng myPos = new LatLng( 46.0829800, 11.1155410 );
        taskManager.startRunningTask( new getTrips( getContext( ) ), true, myPos );
    }

    class getTrips extends CancellableAsyncTask<LatLng, Integer, List<Path>> {

        String exceptionMessage;
        Context context;

        public getTrips( Context context ) {
            super( taskManager );
            this.context = context;
        }

        @Override
        protected List<Path> doInBackground( LatLng... pos ) {

            try {
                List<Path> paths = DAOFactory.getPathDAO( ).getPathsFromPosition(
                        context, pos[ 0 ]
                );
                return paths;
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
            mAdapter = new TripsAdapter( res );
            mRecyclerView.setAdapter( mAdapter );

            mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), mRecyclerView, new ClickListener() {
                @Override
                public void onClick(View view, int position) {

                    //sending data from the recycler view to the sliderLayout
                    Path path = mAdapter.getPath(position);
                    mPActivity.showChoosenPath(path);
                    Intent intent = new Intent( getContext(), MainActivity.class);
                    intent.setAction(INTENT_SELECTED_TRIP);
                    Bundle savedInstanceState;
                    //intent.putExtra(INTENT_SELECTED_TRIP, path);
                    intent.putExtra(INTENT_SELECTED_TRIP, path.getId());
                    startActivity(intent);


                }

                @Override
                public void onLongClick(View view, int position) {

                }
            }));
            super.onPostExecute( res );
        }
    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private TripsFragment.ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final TripsFragment.ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }
}

