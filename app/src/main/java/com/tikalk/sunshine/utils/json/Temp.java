
package com.tikalk.sunshine.utils.json;

import java.util.HashMap;
import java.util.Map;

public class Temp {

    private Double day;
    private Double min;
    private Double max;
    private Double night;
    private Double eve;
    private Double morn;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The day
     */
    public Double getDay() {
        return day;
    }

    /**
     * 
     * @param day
     *     The day
     */
    public void setDay(Double day) {
        this.day = day;
    }

    public Temp withDay(Double day) {
        this.day = day;
        return this;
    }

    /**
     * 
     * @return
     *     The min
     */
    public Double getMin() {
        return min;
    }

    /**
     * 
     * @param min
     *     The min
     */
    public void setMin(Double min) {
        this.min = min;
    }

    public Temp withMin(Double min) {
        this.min = min;
        return this;
    }

    /**
     * 
     * @return
     *     The max
     */
    public Double getMax() {
        return max;
    }

    /**
     * 
     * @param max
     *     The max
     */
    public void setMax(Double max) {
        this.max = max;
    }

    public Temp withMax(Double max) {
        this.max = max;
        return this;
    }

    /**
     * 
     * @return
     *     The night
     */
    public Double getNight() {
        return night;
    }

    /**
     * 
     * @param night
     *     The night
     */
    public void setNight(Double night) {
        this.night = night;
    }

    public Temp withNight(Double night) {
        this.night = night;
        return this;
    }

    /**
     * 
     * @return
     *     The eve
     */
    public Double getEve() {
        return eve;
    }

    /**
     * 
     * @param eve
     *     The eve
     */
    public void setEve(Double eve) {
        this.eve = eve;
    }

    public Temp withEve(Double eve) {
        this.eve = eve;
        return this;
    }

    /**
     * 
     * @return
     *     The morn
     */
    public Double getMorn() {
        return morn;
    }

    /**
     * 
     * @param morn
     *     The morn
     */
    public void setMorn(Double morn) {
        this.morn = morn;
    }

    public Temp withMorn(Double morn) {
        this.morn = morn;
        return this;
    }


    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

      public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Temp withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }


}
