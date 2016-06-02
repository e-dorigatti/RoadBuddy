package it.unitn.roadbuddy.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TripsFragment extends Fragment {

    private ArrayAdapter<String> mTripsAdapter;
    public TripsFragment( ) {
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
        View rootView = inflater.inflate(R.layout.fragment_trips, container, false);
        String[] data = {
                "Trip1 - 3 hours - 5stars",
                "Trip2 - 7 hours - 3stars",
                "Trip3 - 1 hours - 4stars",
                "Trip4 - 2 hours - 1stars"
        };


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
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String tripsPos = mTripsAdapter.getItem(position);
                Intent detailIntent;
                detailIntent = new Intent(getActivity(), TripsDetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, tripsPos);
                startActivity(detailIntent);

            }
        });
        return rootView;
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
