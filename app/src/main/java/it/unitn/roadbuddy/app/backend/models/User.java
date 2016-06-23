package it.unitn.roadbuddy.app.backend.models;


import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

public class User implements Parcelable {
    public static final Parcelable.Creator<User> CREATOR
            = new Parcelable.Creator<User>( ) {

        public User createFromParcel( Parcel in ) {
            return new User( in );
        }

        public User[] newArray( int size ) {
            return new User[ size ];
        }
    };

    private int id;
    private String userName;
    private Date lastPositionUpdated;
    private Integer trip;
    private transient LatLng lastPosition;

    public User( int id, String userName, LatLng lastPosition,
                 Date lastPositionUpdated, Integer trip ) {

        this.id = id;
        this.userName = userName;
        this.lastPosition = lastPosition;
        this.lastPositionUpdated = lastPositionUpdated;
        this.trip = trip;
    }

    public User( Parcel parcel ) {
        this.id = parcel.readInt( );
        this.userName = parcel.readString( );
        this.lastPosition = parcel.readParcelable( ClassLoader.getSystemClassLoader( ) );
        this.lastPositionUpdated = ( Date ) parcel.readSerializable( );
        this.trip = ( Integer ) parcel.readValue( ClassLoader.getSystemClassLoader( ) );
    }

    @Override
    public void writeToParcel( Parcel parcel, int i ) {
        parcel.writeInt( id );
        parcel.writeString( userName );
        parcel.writeValue( trip );
        parcel.writeParcelable( lastPosition, 0 );
        parcel.writeSerializable( lastPositionUpdated );
    }

    @Override
    public int describeContents( ) {
        return 0;
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
