package it.unitn.roadbuddy.app.backend.models;


import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

public class User {
    private long id;
    private String userName;
    private LatLng lastPosition;
    private Date lastPositionUpdated;

    public User( long id, String userName, LatLng lastPosition, Date lastPositionUpdated ) {
        this.id = id;
        this.userName = userName;
        this.lastPosition = lastPosition;
        this.lastPositionUpdated = lastPositionUpdated;
    }

    public long getId( ) {
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
}
