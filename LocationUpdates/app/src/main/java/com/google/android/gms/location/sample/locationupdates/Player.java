package com.google.android.gms.location.sample.locationupdates;

import com.ibm.mobile.services.data.IBMDataObject;
import com.ibm.mobile.services.data.IBMDataObjectSpecialization;

/**
 * Created by kalathur on 6/3/2015.
 */

@IBMDataObjectSpecialization("Player")
public class Player extends IBMDataObject {

    public static final String CLASS_NAME = "Player";
    //Database fields

    private static final String ID = "id";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";
    private static final String STATE = "state";
    private static final String NUMTAGGED = "numTagged";
    private static final String LASTUPDATETIME = "lastUpdateTime";

    //Possible player stages. Thinking out loud.
    private static final String ALIVE   = "ALIVE";
    //means player is a meat eater
    private static final String ZOMBIE    = "ZOMBIE";
    //means player is ejected from the current game.
    private static final String DEAD      = "DEAD";

    /**
     * Sets the id for the player.
     */
    public void setId(String id) {
        setObject(ID, (id != null) ? id : "");
    }

    /**
     * Gets the id of the player.
     * @return String id
     */
    public String getId() {
        return (String) getObject(ID);
    }

    /**
     * Sets the current state of player .
     */
    public void setState(String status) {
        //Set valid Health states else default to zombie.
        switch( status ){
            case DEAD:
            case ALIVE:
            case ZOMBIE: setObject(STATE, status);
                break;
            default:
                setObject(STATE, ZOMBIE);
                break;
        }
    }

    /**
     * Gets the state of the player.
     * @return String health
     */
    public String getState() {
		/* return health; */
        return (String) getObject(STATE);
    }

    /**
     * Sets the current latitude and longitude of Player .
     */
    public void setLatLong(double la, double lo) {
        setObject(LATITUDE, la);
        setObject(LONGITUDE, lo);
    }

    /**
     * Gets the latitude of the Player.
     * @return double latitude
     */
    public double getLatitude() {
		/* return current coordinates; */
        return (double) getObject(LATITUDE);
    }

    /**
     * Gets the longitude of the Player.
     * @return double longitude
     */
    public double getLongitude() {
		/* return current coordinates; */
        return (double) getObject(LONGITUDE);
    }

    /**
     * Sets the number of players tagged by this player.
     */
    public void setNumTagged(int numTagged) {
        setObject(NUMTAGGED, numTagged);
    }

    /**
     * Gets the number of players tagged by this player.
     * @return int numTagged
     */
    public int getNumTagged() {
        return (int) getObject(NUMTAGGED);
    }

    /**
     * Sets the last location update time for this player.
     */
    public void setLastUpdateTime(long lastUpdateTime) {
        setObject(LASTUPDATETIME, lastUpdateTime);
    }

    /**
     * Gets the last location update time for this player.
     * @return long lastUpdateTime
     */
    public long getLastUpdateTime() {
        return (int) getObject(LASTUPDATETIME);
    }

    /**
     * When calling toString() for a player, we'd really only want the id.
     * @return String playerId
     */
    public String toString() {
        String playerId = "";
        playerId = getId();
        return playerId;
    }
}
