package it.unitn.roadbuddy.app;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Path;


public class TripsFragment extends Fragment {
    ViewPager mPager;
    PagerAdapter mAdapter;
    CancellableAsyncTaskManager taskManager;

    private ArrayAdapter<String> mTripsAdapter;

    public TripsFragment() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        this.mPager = (ViewPager) getActivity().findViewById(R.id.pager);
        this.mAdapter = (PagerAdapter) mPager.getAdapter();
        this.taskManager = new CancellableAsyncTaskManager();

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_trips, container, false);

        String[] data = {
                "Trip1 - 3 hours - 5stars",
                "Trip2 - 7 hours - 3stars",
                "Trip3 - 1 hours - 4stars",
                "Trip4 - 2 hours - 1stars"
        };
        LatLng myPos = new LatLng(46.0829800, 11.1155410);
        taskManager.startRunningTask(new getTrips(getContext()), true, myPos);

        List<String> tripList = new ArrayList<String>(
                Arrays.asList(data));

        mTripsAdapter = new ArrayAdapter<String>(
                //current context
                getActivity(),
                //ID of list of item layout
                R.layout.list_item_trips,
                //ID of the textview to populate
                R.id.list_item_trips_textview,
                //forecast data
                tripList);
        ListView listView = (ListView) rootView.findViewById(R.id.list_view_trips);
        listView.setAdapter(mTripsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                String v = (String) adapter.getAdapter().getItem(position);
                mAdapter.setView(v);
                mPager.setCurrentItem(0);
            }
        });
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onPause() {
        super.onPause();
        taskManager.stopRunningTasksOfType(getTrips.class);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    class getTrips extends CancellableAsyncTask<LatLng, Integer, List<Path>> {

        String exceptionMessage;
        Context context;

        public getTrips(Context context) {
            super(taskManager);
            this.context = context;
        }

        @Override
        protected List<Path> doInBackground(LatLng... pos) {

            try {
                List<Path> paths = DAOFactory.getPathDAO().getPathsFromPosition(
                        context, pos[0]
                );
                return paths;
            } catch (BackendException e) {
                Log.e(getClass().getName(), "while getting trips from position", e);
                exceptionMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Path> res) {
            for ( Path p : res )
                Log.v("res", Long.toString(p.getId()));
            super.onPostExecute(res);
        }
    }
}

