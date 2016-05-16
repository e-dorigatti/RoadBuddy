package it.unitn.roadbuddy.app;


import com.google.android.gms.maps.GoogleMap;

public interface Drawable {
    /**
     * Draws the object on the map.
     * This method will be called only once.
     * Draw the object as initially unselected.
     *
     * @param map map in which to draw the object
     * @return ID of the drawn object
     */
    String DrawToMap( GoogleMap map );

    /**
     * Set whether the object is selected by the user or not.
     * Use this method, for example, to change the appearance
     * of the drawn object.
     *
     * @param selected Whether the object is selected or not
     */
    void setSelected( boolean selected );

    /**
     * Delete the graphic object from the map
     */
    void RemoveFromMap( );

    /**
     * Whether the two drawables share the same underlying object.
     * This should not depend on the appearance
     *
     * @param other Other drawable to compare
     * @return Whether the represented object is the same
     */
    boolean equals( Drawable other );
}
