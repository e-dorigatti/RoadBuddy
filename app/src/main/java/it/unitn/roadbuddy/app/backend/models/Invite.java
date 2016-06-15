package it.unitn.roadbuddy.app.backend.models;


public class Invite {
    private int id;
    private User inviter;
    private User invitee;
    private Trip trip;

    public Invite( int id, User inviter, User invitee, Trip trip ) {
        this.id = id;
        this.inviter = inviter;
        this.invitee = invitee;
        this.trip = trip;
    }

    public int getId( ) {
        return id;
    }

    public User getInviter( ) {
        return inviter;
    }

    public User getInvitee( ) {
        return invitee;
    }

    public Trip getTrip( ) {
        return trip;
    }
}
