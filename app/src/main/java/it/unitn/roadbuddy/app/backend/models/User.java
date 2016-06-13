package it.unitn.roadbuddy.app.backend.models;


import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

public class User {
    private int id;
    private String userName;
    private LatLng lastPosition;
    private Date lastPositionUpdated;
    private Integer trip;

    public User( int id, String userName, LatLng lastPosition,
                 Date lastPositionUpdated, Integer trip ) {

        this.id = id;
        this.userName = userName;
        this.lastPosition = lastPosition;
        this.lastPositionUpdated = lastPositionUpdated;
        this.trip = trip;
    }

    public int getId( ) {
        return id;
    }

    public String getUserName( ) {
        return userName;
    }

    public LatLng getLastPosition( ) {
        return lastPosition;
    }

    public Date getLastPositionUpdated( ) {
        return lastPositionUpdated;
    }

    public Integer getTrip( ) {
        return trip;
    }
}
