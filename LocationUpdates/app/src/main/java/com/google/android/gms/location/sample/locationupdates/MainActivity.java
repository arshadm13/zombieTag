/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.location.sample.locationupdates;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.ibm.mobile.services.data.IBMDataException;
import com.ibm.mobile.services.data.IBMDataObject;
import com.ibm.mobile.services.data.IBMQuery;

import java.util.ArrayList;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

public class MainActivity extends ActionBarActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final String CLASS_NAME = MainActivity.class.getSimpleName();

    protected static final String TAG = "zombie-tag";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 100000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    public Player myPlayer;

    protected List<Player> localPlayersList;
    // UI Widgets.

    protected TextView mPlayerIdTextView;
    protected TextView mPlayerStateTextView;
    protected TextView mLatitudeTextView;
    protected TextView mLongitudeTextView;

    protected TextView mZombiesCreatedTextView;
    protected TextView mZombieCountTextView;
    protected TextView mHumanCountTextView;

    ZombieTagApplication zombieTagApplication;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        localPlayersList = new ArrayList<Player>();

        zombieTagApplication = (ZombieTagApplication) getApplication();

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices API.
        buildGoogleApiClient();

        Player.registerSpecialization(Player.class);

        createLocalPlayersList();

        // Bit of hack for now if we have a large return time from the query.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "Players List = " + localPlayersList);

        myPlayer = new Player();
        myPlayer.setId(getPhoneNumber());
        myPlayer.setState("ALIVE");
        myPlayer.setLatLong(0, 0);
        myPlayer.setNumTagged(0);
        myPlayer.setLastUpdateTime(System.currentTimeMillis());
        if(!doesPlayerExist(myPlayer.getId())) {
            createPlayerDataInDB();
        } else {
            // set myPlayer to the DB instance
            getPlayerDataFromDB();
            // Bit of hack for now if we have a large return time from the query.
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mPlayerIdTextView = (TextView) findViewById((R.id.player_id_text));
        mPlayerStateTextView = (TextView) findViewById((R.id.player_state_text));
        mLatitudeTextView = (TextView) findViewById((R.id.latitude_text));
        mLongitudeTextView = (TextView) findViewById((R.id.longitude_text));

        mZombiesCreatedTextView = (TextView) findViewById((R.id.zombies_created_text));
        mZombieCountTextView = (TextView) findViewById((R.id.zombie_count_text));
        mHumanCountTextView = (TextView) findViewById((R.id.human_count_text));

        updateUI();
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }

        // Grab the latest data from the DB and update UI when app is resumed.
        getPlayerDataFromDB();
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        startLocationUpdates();

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation != null && myPlayer != null) {
            myPlayer.setLatLong(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            myPlayer.setLastUpdateTime(System.currentTimeMillis());
            updatePlayerDataInDB();
            updateUI();
        }
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        if(myPlayer != null && location != null) {
            myPlayer.setLatLong(location.getLatitude(), location.getLongitude());
            myPlayer.setLastUpdateTime(System.currentTimeMillis());
        }

        // When location changes update the UI & DB
        updatePlayerDataInDB();
        updateUI();
        //Toast.makeText(this, getResources().getString(R.string.location_updated_message), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    /**************************  Local methods to manage the game  *******************************/

    /*
     * Grab the phone number to use as the user's id for the game.
     * Temporary solution til we implement a login module for the game.
     */
    private String getPhoneNumber() {
        TelephonyManager tMgr =(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        return tMgr.getLine1Number();
    }

    public void createPlayerDataInDB() {
        // Use the IBMDataObject to create and persist the Item object.
        myPlayer.save().continueWith(new Continuation<IBMDataObject, Void>() {

            @Override
            public Void then(Task<IBMDataObject> task) throws Exception {
                // Log if the save was cancelled.
                if (task.isCancelled()) {
                    Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                }
                // Log error message, if the save task fails.
                else if (task.isFaulted()) {
                    Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                }

                return null;
            }
        });
        Log.e(TAG, "Created player + " + myPlayer.getId() + " in Mobile Data");
    }

    /*
     * Update the users data locally from the latest data in the DB.
     */
    public void getPlayerDataFromDB() {
        try {
            IBMQuery<Player> query = IBMQuery.queryForClass(Player.class);
            // Query all the Item objects from the server
            query.find().continueWith(new Continuation<List<Player>, Void>() {

                @Override
                public Void then(Task<List<Player>> task) throws Exception {
                    final List<Player> objects = task.getResult();
                    // Log if the find was cancelled.
                    if (task.isCancelled()){
                        Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                    }
                    // Log error message, if the find task fails.
                    else if (task.isFaulted()) {
                        Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                    }

                    // If the result succeeds, load the list.
                    else {
                        // We'll be reordering and repopulating from DataService.
                        for(IBMDataObject player:objects) {
                            Player p = (Player) player;
                            if (p.getId().equals(myPlayer.getId()))
                            {
                                Log.i(CLASS_NAME, "Found player " + p.getId() + " ( " + p.getState() + " ) in the DB and refreshing player's local data");
                                myPlayer = p;
                            }
                        }
                    }
                    return null;
                }
            },Task.UI_THREAD_EXECUTOR);

        }  catch (IBMDataException error) {
            Log.e(CLASS_NAME, "Exception : " + error.getMessage());
        }
    }

    private void updatePlayerDataInDB() {
        // Use the IBMDataObject to create and persist the Item object.
        myPlayer.save().continueWith(new Continuation<IBMDataObject, Void>() {

            @Override
            public Void then(Task<IBMDataObject> task) throws Exception {
                // Log if the save was cancelled.
                if (task.isCancelled()) {
                    Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                }
                // Log error message, if the save task fails.
                else if (task.isFaulted()) {
                    Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                }

                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

        Log.e(TAG, "Updated player + " + myPlayer.getId() + " in Mobile Data");
    }

    private void updatePlayerDataInDB(Player player) {
        // Use the IBMDataObject to create and persist the Item object.
        player.save().continueWith(new Continuation<IBMDataObject, Void>() {

            @Override
            public Void then(Task<IBMDataObject> task) throws Exception {
                // Log if the save was cancelled.
                if (task.isCancelled()) {
                    Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                }
                // Log error message, if the save task fails.
                else if (task.isFaulted()) {
                    Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                }

                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

        Log.e(TAG, "Updated player + " + player + " in Mobile Data");
    }

    /*
     * Update the users data locally from the latest data in the DB.
     */
    public void createLocalPlayersList() {
        localPlayersList.clear();
        try {
            IBMQuery<Player> query = IBMQuery.queryForClass(Player.class);
            // Query all the Item objects from the server
            query.find().continueWith(new Continuation<List<Player>, Void>() {

                @Override
                public Void then(Task<List<Player>> task) throws Exception {
                    final List<Player> objects = task.getResult();
                    // Log if the find was cancelled.
                    if (task.isCancelled()){
                        Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                    }
                    // Log error message, if the find task fails.
                    else if (task.isFaulted()) {
                        Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                    }

                    // If the result succeeds, load the list.
                    else {
                        // We'll be reordering and repopulating from DataService.
                        for(IBMDataObject player:objects) {
                            //TODO: Conditionally add player;
                            localPlayersList.add((Player) player);
                            Log.e(TAG, "Adding " + ((Player) player).getId() + " to local list.");
                        }
                    }
                    return null;
                }
            });

            updateUI();
        }  catch (IBMDataException error) {
            Log.e(CLASS_NAME, "Exception : " + error.getMessage());
        }
    }

    /**
     * Update the players current profile in the UI.
     */
    protected void updateUI() {
        if (myPlayer != null) {
            mPlayerIdTextView.setText(myPlayer.getId());
            mPlayerStateTextView.setText(myPlayer.getState());
            mLatitudeTextView.setText(String.valueOf(myPlayer.getLatitude()));
            mLongitudeTextView.setText(String.valueOf(myPlayer.getLongitude()));

            mZombiesCreatedTextView.setText(String.valueOf(myPlayer.getNumTagged()));
            mZombieCountTextView.setText(String.valueOf(getZombieCount()));
            mHumanCountTextView.setText(String.valueOf(getHumanCount()));
        }
    }

    private boolean doesPlayerExist(String playerName) {
        for (Player player:localPlayersList) {
            if (player.getId().equals(playerName)) {
                Log.e(TAG, "doesPlayerExist = true");
                return true;
            }
        }

        Log.e(TAG, "doesPlayerExist = false");
        return false;
    }

    private int getZombieCount() {
        int zombieCount = 0;
        for (Player player:localPlayersList) {
            if (player.getState().equals("ZOMBIE")) {
                zombieCount++;
            }
        }

        Log.e(TAG, "zombieCount = " + zombieCount);
        return zombieCount;
    }

    private int getHumanCount() {
        int humanCount = 0;
        for (Player player:localPlayersList) {
            if (player.getState().equals("ALIVE")) {
                humanCount++;
            }
        }

        Log.e(TAG, "humanCount = " + humanCount);
        return humanCount;
    }
}