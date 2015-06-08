/*
 * Copyright 2014 IBM Corp. All Rights Reserved
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.ibm.mobile.services.core.IBMBluemix;
import com.ibm.mobile.services.data.IBMData;
import com.ibm.mobile.services.push.IBMPush;
import com.ibm.mobile.services.push.IBMPushNotificationListener;
import com.ibm.mobile.services.push.IBMSimplePushNotification;

import bolts.Continuation;
import bolts.Task;

public final class ZombieTagApplication extends Application {
    public static final int EDIT_ACTIVITY_RC = 1;
    public static IBMPush push = null;
    private Activity mActivity;
    private static final String deviceAlias = "TargetDevice";
    private static final String consumerID = "MBaaSListApp";
    private static final String CLASS_NAME = ZombieTagApplication.class.getSimpleName();
    private static final String APP_ID = "085c1092-7f59-4d95-bec3-17dbf530c015";
    private static final String APP_SECRET = "5ea91f57f46ce0b0bac8ec81905d4542c680b78e";
    private static final String APP_ROUTE = "http://vijai-zombietag.mybluemix.net";

    private IBMPushNotificationListener notificationListener = null;

    public ZombieTagApplication() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity,Bundle savedInstanceState) {
                Log.d(CLASS_NAME, "Activity created: " + activity.getLocalClassName());
                mActivity = activity;

                // Define IBMPushNotificationListener behavior on push notifications.
                notificationListener = new IBMPushNotificationListener() {
                    @Override
                    public void onReceive(final IBMSimplePushNotification message) {
                        mActivity.runOnUiThread(new Runnable(){
                            @Override
                            public void run() {
                                Class<? extends Activity> actClass = mActivity.getClass();
                                if (actClass == MainActivity.class) {

                                    Log.e(CLASS_NAME, "Notification message received: " + message.toString());
                                    if (message.getAlert() != null) {
                                        // Present the message when sent from Push notification console.
                                        if (message.getAlert().contains("zombie")) {
                                            ((MainActivity)mActivity).getPlayerDataFromDB();
                                            ((MainActivity)mActivity).createLocalPlayersList();
                                            ((MainActivity)mActivity).updateUI();
                                            mActivity.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    new AlertDialog.Builder(mActivity)
                                                            .setTitle("Zombie ALERT!!!")
                                                            .setMessage(message.getAlert())
                                                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                                }
                                                            })
                                                            .show();
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        });
                    }
                };
            }
            @Override
            public void onActivityStarted(Activity activity) {
                mActivity = activity;
                Log.d(CLASS_NAME, "Activity started: " + activity.getLocalClassName());
            }
            @Override
            public void onActivityResumed(Activity activity) {
                mActivity = activity;
                Log.d(CLASS_NAME, "Activity resumed: " + activity.getLocalClassName());
                if (push != null) {
                    push.listen(notificationListener);
                }
            }
            @Override
            public void onActivitySaveInstanceState(Activity activity,Bundle outState) {
                Log.d(CLASS_NAME, "Activity saved instance state: " + activity.getLocalClassName());
            }
            @Override
            public void onActivityPaused(Activity activity) {
                if (push != null) {
                    push.hold();
                }
                Log.d(CLASS_NAME, "Activity paused: " + activity.getLocalClassName());
                if (activity != null && activity.equals(mActivity))
                    mActivity = null;
            }
            @Override
            public void onActivityStopped(Activity activity) {
                Log.d(CLASS_NAME, "Activity stopped: " + activity.getLocalClassName());
            }
            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.d(CLASS_NAME, "Activity destroyed: " + activity.getLocalClassName());
            }
        });
    }

    /**
     *
     * Called when the application is starting, before any activity, service,
     * or receiver objects (excluding content providers) have been created.
     * Register the Bluemix services.
     *
     * @see android.app.Application#onCreate()
     *
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the IBM core backend-as-a-service.
        IBMBluemix.initialize(this, APP_ID, APP_SECRET, APP_ROUTE);
        // Initialize the IBM Data Service.
        IBMData.initializeService();
        // Initialize IBM Push service.
        IBMPush.initializeService();
        // Retrieve instance of the IBM Push service.
        push = IBMPush.getService();
        // Register the device with the IBM Push service.
        push.register(deviceAlias, consumerID).continueWith(new Continuation<String, Void>() {
            @Override
            public Void then(Task<String> task) throws Exception {
                if (task.isCancelled()) {
                    Log.e(CLASS_NAME, "Exception : Task " + task.toString() + " was cancelled.");
                } else if (task.isFaulted()) {
                    Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                } else {
                    Log.d(CLASS_NAME, "Device Successfully Registered");
                }

                return null;
            }
        });
    }
}