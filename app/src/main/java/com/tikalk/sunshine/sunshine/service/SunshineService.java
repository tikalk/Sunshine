package com.tikalk.sunshine.sunshine.service;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.tikalk.sunshine.sunshine.BuildConfig;
import com.tikalk.sunshine.sunshine.ForecastFragment;
import com.tikalk.sunshine.sunshine.data.db.WeatherContract;
import com.tikalk.sunshine.utils.Utility;
import com.tikalk.sunshine.utils.json.Temp;
import com.tikalk.sunshine.utils.json.Weather;
import com.tikalk.sunshine.utils.json.WeatherData;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

/**
 * Created by oren on 21/01/16.
 */
public class SunshineService extends IntentService {

    public SunshineService() {
        super("SunshineService");
    }

    private final String LOG_TAG = SunshineService.class.getSimpleName();

    private final OkHttpClient client = new OkHttpClient();


    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName        A human-readable city name, e.g "Mountain View"
     * @param lat             the latitude of the city
     * @param lon             the longitude of the city
     * @return the row ID of the added location.
     */
    public long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // Students: First, check if the location with this city name exists in the db
        Cursor locationCursor = null;
        try {
            locationCursor = getContentResolver().query(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    new String[]{
                            WeatherContract.LocationEntry._ID,
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
        Uri inserted = getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, contentValues);
        return ContentUris.parseId(inserted);
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
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
            getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

            String locationQuery = intent.getExtras().getString(ForecastFragment.LOCATION_TAG);
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
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            forecastJsonStr = response.body().string();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return;
        }
        try {
            getWeatherDataFromJson(forecastJsonStr, locationQuery);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        // This will only happen if there was an error getting or parsing the forecast.
        return;
    }

    public static class AlarmReceiver extends BroadcastReceiver {
        private final String LOG_TAG = AlarmReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "Intent Received");
            Intent serviceIntent = new Intent(context,SunshineService.class);
            serviceIntent.putExtra(ForecastFragment.LOCATION_TAG, Utility.getPreferredLocation(context));
            context.startService(serviceIntent);
        }
    }
}
