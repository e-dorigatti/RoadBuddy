package it.unitn.roadbuddy.app.backend;

import it.unitn.roadbuddy.app.backend.models.Invite;

import java.util.List;

public interface InviteDAO {
    boolean addInvite( int inviter, String invitee, int trip ) throws BackendException;

    List<Invite> retrieveInvites( int user ) throws BackendException;

    void removeInvite( int invite ) throws BackendException;
}
