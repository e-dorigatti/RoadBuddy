package it.unitn.roadbuddy.app.backend;

import java.util.List;

public interface InviteDAO {
    boolean addInvite( int inviter, String invitee, int trip ) throws BackendException;

    List<Integer> retrieveInvites( int user ) throws BackendException;
}
