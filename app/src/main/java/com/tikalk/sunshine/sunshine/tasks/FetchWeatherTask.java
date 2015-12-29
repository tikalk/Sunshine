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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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
import com.tikalk.sunshine.sunshine.R;
import com.tikalk.sunshine.sunshine.data.db.WeatherContract;
import com.tikalk.sunshine.utils.Temp;
import com.tikalk.sunshine.utils.Weather;
import com.tikalk.sunshine.utils.WeatherData;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static com.tikalk.sunshine.sunshine.data.db.WeatherContract.WeatherEntry;

public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private ArrayAdapter<String> mForecastAdapter;
    private final Context mContext;
    private final OkHttpClient client = new OkHttpClient();
    private double cityLatitude;

    public FetchWeatherTask(Context context, ArrayAdapter<String> forecastAdapter) {
        mContext = context;
        mForecastAdapter = forecastAdapter;
    }

    private boolean DEBUG = true;

    /* The date/time conversion code is going to be moved outside the asynctask later,
     * so for convenience we're breaking it out into its own method now.
     */
    private String getReadableDateString(long time) {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // Data is fetched in Celsius by default.
        // If user prefers to see in Fahrenheit, convert the values here.
        // We do this rather than fetching in Fahrenheit so that the user can
        // change this option without us having to re-fetch the data once
        // we start storing the values in a database.
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(mContext);
        String unitType = sharedPrefs.getString(
                mContext.getString(R.string.pref_temp_unit_key),
                mContext.getString(R.string.pref_temp_units_metric));

        if (unitType.equals(mContext.getString(R.string.pref_temp_units_imperial))) {
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        } else if (!unitType.equals(mContext.getString(R.string.pref_temp_units_metric))) {
            Log.d(LOG_TAG, "Unit type not found: " + unitType);
        }

        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
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
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        return -1;
    }

    /*
        Students: This code will allow the FetchWeatherTask to continue to return the strings that
        the UX expects so that we can continue to test the application even once we begin using
        the database.
     */
    String[] convertContentValuesToUXFormat(Vector<ContentValues> cvv) {
        // return strings to keep UI functional for now
        String[] resultStrs = new String[cvv.size()];
        for (int i = 0; i < cvv.size(); i++) {
            ContentValues weatherValues = cvv.elementAt(i);
            String highAndLow = formatHighLows(
                    weatherValues.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP),
                    weatherValues.getAsDouble(WeatherEntry.COLUMN_MIN_TEMP));
            resultStrs[i] = getReadableDateString(
                    weatherValues.getAsLong(WeatherEntry.COLUMN_DATE)) +
                    " - " + weatherValues.getAsString(WeatherEntry.COLUMN_SHORT_DESC) +
                    " - " + highAndLow;
        }
        return resultStrs;
    }

    private double converToImperial(double celiusValue) {
        return celiusValue * 1.8 + 32;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr,
                                            String locationSetting, boolean isMetric)
            throws JSONException {

        Gson gson = new GsonBuilder().create();
        WeatherData weatherData = gson.fromJson(forecastJsonStr, WeatherData.class);
        Calendar calendar = Calendar.getInstance();
        Vector<ContentValues> cVVector = new Vector<>(weatherData.getList().size());

        for (com.tikalk.sunshine.utils.List list : weatherData.getList()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            final Temp temp = list.getTemp();
            final double max = isMetric ? temp.getMax() : converToImperial(temp.getMax());
            final double min = isMetric ? temp.getMin() : converToImperial(temp.getMin());
            String mainWeather = list.getWeather().iterator().next().getMain();
            ContentValues weatherValues = new ContentValues();
            List<Weather> weatherList = list.getWeather();
            Weather weather = weatherList.iterator().next();
            String cityName = weatherData.getCity().getName();
            double cityLatitude = weatherData.getCity().getCoord().getLat();
            double cityLongitude = weatherData.getCity().getCoord().getLon();

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);
            weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationId);
            weatherValues.put(WeatherEntry.COLUMN_DATE, list.getDt());
            weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, list.getHumidity());
            weatherValues.put(WeatherEntry.COLUMN_PRESSURE, list.getPressure());
            weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, list.getSpeed());
            weatherValues.put(WeatherEntry.COLUMN_DEGREES, list.getDeg());
            weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, max);
            weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, min);
            weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, weather.getDescription());
            weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weather.getId());

            cVVector.add(weatherValues);
        }

        // add to database
        if (cVVector.size() > 0) {
            // Student: call bulkInsert to add the weatherEntries to the database here
        }

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        // Students: Uncomment the next lines to display what what you stored in the bulkInsert

//            Cursor cur = mContext.getContentResolver().query(weatherForLocationUri,
//                    null, null, null, sortOrder);
//
//            cVVector = new Vector<ContentValues>(cur.getCount());
//            if ( cur.moveToFirst() ) {
//                do {
//                    ContentValues cv = new ContentValues();
//                    DatabaseUtils.cursorRowToContentValues(cur, cv);
//                    cVVector.add(cv);
//                } while (cur.moveToNext());
//            }
        String[] resultStrs = convertContentValuesToUXFormat(cVVector);
        return resultStrs;

    }


    @Override
    protected String[] doInBackground(String... params) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String location = prefs.getString(mContext.getString(R.string.pref_location_key),mContext.getString(R.string.pref_location_defualt));
        String tempUnit = prefs.getString(mContext.getString(R.string.pref_temp_unit_key), mContext.getString((R.string.pref_temp_units_default)));
        boolean isMetric = tempUnit.equals(mContext.getString((R.string.pref_temp_units_metric)));
        // If there's no zip code, there's nothing to look up.  Verify size of params.
//        if (params.length == 0) {
//            return null;
//        }
        String locationQuery = location;


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

            return getWeatherDataFromJson(forecastJsonStr, locationQuery, units.equals("metric"));
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        // This will only happen if there was an error getting or parsing the forecast.
        return null;
    }

    @Override
    protected void onPostExecute(String[] result) {
        if (result != null && mForecastAdapter != null) {
            mForecastAdapter.clear();
            for (String dayForecastStr : result) {
                mForecastAdapter.add(dayForecastStr);
            }
            // New data is back from the server.  Hooray!
        }
    }
}