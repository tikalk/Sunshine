/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tikalk.sunshine.sunshine.tasks;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.tikalk.sunshine.sunshine.BuildConfig;
import com.tikalk.sunshine.sunshine.ForecastAdapter;
import com.tikalk.sunshine.sunshine.R;
import com.tikalk.sunshine.sunshine.data.db.WeatherContract;
import com.tikalk.sunshine.utils.json.Temp;
import com.tikalk.sunshine.utils.json.Weather;
import com.tikalk.sunshine.utils.json.WeatherData;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static com.tikalk.sunshine.sunshine.data.db.WeatherContract.WeatherEntry;

public class FetchWeatherTask extends AsyncTask<String, Void, Void> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private final Context mContext;
    private final OkHttpClient client = new OkHttpClient();

    public FetchWeatherTask(Context context) {
        mContext = context;

    }


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
            locationCursor = mContext.getContentResolver().query(
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
        Uri inserted = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, contentValues);
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
            weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationId);
            weatherValues.put(WeatherEntry.COLUMN_DATE, calendar.getTime().getTime());
            weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, list.getHumidity());
            weatherValues.put(WeatherEntry.COLUMN_PRESSURE, list.getPressure());
            weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, list.getSpeed());
            weatherValues.put(WeatherEntry.COLUMN_DEGREES, list.getDeg());
            weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, max);
            weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, min);
            weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, weather.getDescription());
            weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weather.getId());

            cVVector.add(weatherValues);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // add to database
        if (cVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            mContext.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, cvArray);
        }
    }


    @Override
    protected Void doInBackground(String... params) {

        String locationQuery = params[0];


        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

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
            return null;
        }
        try {
            getWeatherDataFromJson(forecastJsonStr, locationQuery);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        // This will only happen if there was an error getting or parsing the forecast.
        return null;
    }

}