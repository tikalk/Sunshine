package com.tikalk.sunshine.sunshine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.tikalk.sunshine.sunshine.BuildConfig;
import com.tikalk.sunshine.sunshine.MainActivity;
import com.tikalk.sunshine.sunshine.R;
import com.tikalk.sunshine.sunshine.data.db.WeatherContract;
import com.tikalk.sunshine.utils.Utility;
import com.tikalk.sunshine.utils.json.Temp;
import com.tikalk.sunshine.utils.json.Weather;
import com.tikalk.sunshine.utils.json.WeatherData;

import org.json.JSONException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;


public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();
    private final OkHttpClient client = new OkHttpClient();
    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    public static final int LOCATION_STATUS_OK = 0;
    public static final int LOCATION_STATUS_SERVER_DOWN = 1;
    public static final int LOCATION_STATUS_SERVER_INVALID = 2;
    public static final int LOCATION_STATUS_UNKNOWN = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOCATION_STATUS_OK, LOCATION_STATUS_SERVER_DOWN, LOCATION_STATUS_SERVER_INVALID, LOCATION_STATUS_UNKNOWN})
    public @interface LocationStatus {
    }

    private int locationStatus;



    @LocationStatus
    public int getLocationStatus() {
        return locationStatus;
    }

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "onPerformSync Called.");
        String locationQuery = Utility.getPreferredLocation(getContext());

        String forecastJsonStr;

        String format = "json";
        final String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String ID_PARAM = "id";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";
            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(ID_PARAM, locationQuery)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            Request request = new Request.Builder().url(url).build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                setLocationStatus(LOCATION_STATUS_SERVER_DOWN);
                Log.e(LOG_TAG, "Error "+response.message());
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return;

            }
            forecastJsonStr = response.body().string();
            if (forecastJsonStr == null || forecastJsonStr.isEmpty()){
                setLocationStatus(LOCATION_STATUS_SERVER_INVALID);
                Log.e(LOG_TAG, "Error empty response from seerver");
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return;
            }
            notifyWeather();
        } catch (IOException e) {
            setLocationStatus(LOCATION_STATUS_SERVER_DOWN);
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return;
        }
        try {
            getWeatherDataFromJson(forecastJsonStr, locationQuery);
        } catch (JSONException e) {
            setLocationStatus(LOCATION_STATUS_SERVER_INVALID);
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
            return;
        }
        setLocationStatus(LOCATION_STATUS_OK);
        // This will only happen if there was an error getting or parsing the forecast.
        return;

    }

    private void setLocationStatus(@LocationStatus int location) {
        Utility.setLocationStatus(getContext(),location);
    }

    private void getWeatherDataFromJson(String forecastJsonStr,
                                        String locationSetting)
            throws JSONException {

        Gson gson = new GsonBuilder().create();
        WeatherData weatherData = gson.fromJson(forecastJsonStr, WeatherData.class);
        Calendar calendar = Calendar.getInstance();
        Vector<ContentValues> cVVector = new Vector<>(weatherData.getList().size());

        for (com.tikalk.sunshine.utils.json.List list : weatherData.getList()) {

            final Temp temp = list.getTemp();
            final double max = temp.getMax();
            final double min = temp.getMin();
            ContentValues weatherValues = new ContentValues();
            List<Weather> weatherList = list.getWeather();
            Weather weather = weatherList.iterator().next();
            String cityName = weatherData.getCity().getName();
            double cityLatitude = weatherData.getCity().getCoord().getLat();
            double cityLongitude = weatherData.getCity().getCoord().getLon();

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, calendar.getTime().getTime());
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, list.getHumidity());
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, list.getPressure());
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, list.getSpeed());
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, list.getDeg());
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, max);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, min);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, weather.getDescription());
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weather.getId());

            cVVector.add(weatherValues);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // add to database
        if (cVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            getContext().getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
        }
        Calendar delCalendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -8);
        getContext().getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,
                WeatherContract.WeatherEntry.COLUMN_DATE + "<?", new String[]{Long.toString(delCalendar.getTime().getTime())});
    }


    public long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // Students: First, check if the location with this city name exists in the db
        Cursor locationCursor = null;
        try {
            locationCursor = getContext().getContentResolver().query(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    new String[]{WeatherContract.LocationEntry._ID,
                    },
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                    new String[]{locationSetting},
                    null);

            // these match the indices of the projection
            if (locationCursor.moveToFirst()) {
                return locationCursor.getLong(locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID));
            }
        } finally {
            if (locationCursor != null) {
                locationCursor.close();
            }
        }
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        ContentValues contentValues = new ContentValues();
        contentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
        Uri inserted = getContext().getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, contentValues);
        return ContentUris.parseId(inserted);
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

        }
        return newAccount;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    private void notifyWeather() {

        try {
            Context context = getContext();
            //checking the last update and notify if it' the first of the day
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (Utility.showNotifications(context)) {
                String lastNotificationKey = context.getString(R.string.pref_last_notification);
                long lastSync = prefs.getLong(lastNotificationKey, 0);

                if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                    // Last sync was more than 1 day ago, let's send a notification with the weather.
                    String locationQuery = Utility.getPreferredLocation(context);

                    Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

                    // we'll query our contentProvider, as always
                    Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

                    if (cursor.moveToFirst()) {
                        int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                        double high = cursor.getDouble(INDEX_MAX_TEMP);
                        double low = cursor.getDouble(INDEX_MIN_TEMP);
                        String desc = cursor.getString(INDEX_SHORT_DESC);

                        int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                        String title = context.getString(R.string.app_name);

                        // Define the text of the forecast.
                        String highTempStr = Utility.formatTemperature(context, high);
                        String lowTempStr = Utility.formatTemperature(context, low);
                        String frmtString = context.getString(R.string.format_notification);
//                    String contentText = String.format(frmtString, desc, highTempStr, lowTempStr);
                        String contentText = "Forecast :" + desc + " high :" + highTempStr + " low :" + lowTempStr;
                        //build your notification here.
                        android.support.v4.app.NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(context)
                                        .setSmallIcon(iconId)
                                        .setContentTitle(title)
                                        .setContentText(contentText);

                        Intent resultIntent = new Intent(context, MainActivity.class);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.in
                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
// Adds the back stack for the Intent (but not the Intent itself)
                        stackBuilder.addParentStack(MainActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
                        stackBuilder.addNextIntent(resultIntent);
                        PendingIntent resultPendingIntent =
                                stackBuilder.getPendingIntent(
                                        0,
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                );
                        mBuilder.setContentIntent(resultPendingIntent);
                        NotificationManager mNotificationManager =
                                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.

                        mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());
                    }

                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();


                }
            }
        } catch (Exception ex) {
            Log.d(LOG_TAG, ex.getMessage(), ex);
            ex.printStackTrace();
        }

    }

}