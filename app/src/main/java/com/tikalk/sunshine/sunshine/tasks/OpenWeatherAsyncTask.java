package com.tikalk.sunshine.sunshine.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.format.Time;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.tikalk.sunshine.sunshine.BuildConfig;
import com.tikalk.sunshine.sunshine.R;
import com.tikalk.sunshine.utils.Temp;
import com.tikalk.sunshine.utils.WeatherData;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Oren on 11/12/2015.
 */
public class OpenWeatherAsyncTask extends AsyncTask<Void, Void, String> {
    public static final Integer DEFAULT_DAYS_FORWARD = 7;
    public static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("EEE, MMM d");
    public static final String WEATHER_DATA = " {0} : {1} , {2}-{3} ";
    private ArrayAdapter<String> arrayAdapter;
    private List<String> result;
    private final OkHttpClient client = new OkHttpClient();
    private Context context;

    public OpenWeatherAsyncTask(ArrayAdapter<String> arrayAdapter, Context context) {
        super();
        this.arrayAdapter = arrayAdapter;
        this.context = context;

    }

    @Override
    protected String doInBackground(Void... params) {
        this.result = null;
        String forecastJsonStr;
        try {
            final String format = "json";
            final String units = "metric";
            final String FORECAST_BASE_URL =   "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String ID_PARAM = "id";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String location = prefs.getString(context.getString(R.string.pref_location_key),context.getString(R.string.pref_location_defualt));
            String tempUnit = prefs.getString(context.getString(R.string.pref_temp_unit_key), context.getString((R.string.pref_temp_units_default)));
            boolean isMetric = tempUnit.equals(context.getString((R.string.pref_temp_units_metric)));
            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(ID_PARAM, location)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, DEFAULT_DAYS_FORWARD.toString())
                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();
            URL url = new URL(builtUri.toString());
            Request request = new Request.Builder().url(url).build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            forecastJsonStr = response.body().string();
            parseForecastJson(forecastJsonStr,isMetric);
        } catch (IOException e) {
            Log.e("PlaceholderFragment", "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        }
        return forecastJsonStr;
    }

    private double converToImperial(double celiusValue){
        return celiusValue * 1.8 + 32;
    }

    private void parseForecastJson(String forecastJsonStr, boolean isMetric) {
        Gson gson = new GsonBuilder().create();
        WeatherData weatherData = gson.fromJson(forecastJsonStr, WeatherData.class);
        result = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for (com.tikalk.sunshine.utils.List list : weatherData.getList()) {
            String formattedDate = getDate(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            final Temp temp = list.getTemp();
            final double max = isMetric ? temp.getMax() : converToImperial(temp.getMax());
            final double min = isMetric ? temp.getMin() : converToImperial(temp.getMin());
            String mainWeather = list.getWeather().iterator().next().getMain();
            final String resultForDay = MessageFormat.format(WEATHER_DATA, formattedDate, mainWeather, Math.round(max), Math.round(min));
            result.add(resultForDay);
            Log.i(OpenWeatherAsyncTask.class.getName(), resultForDay);
        }

    }

    @NonNull
    private String getDate(Date date) {

        return DEFAULT_DATE_FORMAT.format(date);
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (result != null && !result.isEmpty()) {
            this.arrayAdapter.clear();
            this.arrayAdapter.addAll(result);
        }
    }
}