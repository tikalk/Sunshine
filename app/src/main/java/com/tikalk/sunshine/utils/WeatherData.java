
package com.tikalk.sunshine.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
public class WeatherData {

    private City city;
    private String cod;
    private Double message;
    private Integer cnt;
    private java.util.List<com.tikalk.sunshine.utils.List> list = new ArrayList<com.tikalk.sunshine.utils.List>();
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The city
     */
    public City getCity() {
        return city;
    }

    /**
     * 
     * @param city
     *     The city
     */
    public void setCity(City city) {
        this.city = city;
    }

    public WeatherData withCity(City city) {
        this.city = city;
        return this;
    }

    /**
     * 
     * @return
     *     The cod
     */
    public String getCod() {
        return cod;
    }

    /**
     * 
     * @param cod
     *     The cod
     */
    public void setCod(String cod) {
        this.cod = cod;
    }

    public WeatherData withCod(String cod) {
        this.cod = cod;
        return this;
    }

    /**
     * 
     * @return
     *     The message
     */
    public Double getMessage() {
        return message;
    }

    /**
     * 
     * @param message
     *     The message
     */
    public void setMessage(Double message) {
        this.message = message;
    }

    public WeatherData withMessage(Double message) {
        this.message = message;
        return this;
    }

    /**
     * 
     * @return
     *     The cnt
     */
    public Integer getCnt() {
        return cnt;
    }

    /**
     * 
     * @param cnt
     *     The cnt
     */
    public void setCnt(Integer cnt) {
        this.cnt = cnt;
    }

    public WeatherData withCnt(Integer cnt) {
        this.cnt = cnt;
        return this;
    }

    /**
     * 
     * @return
     *     The list
     */
    public java.util.List<com.tikalk.sunshine.utils.List> getList() {
        return list;
    }

    /**
     * 
     * @param list
     *     The list
     */
    public void setList(java.util.List<com.tikalk.sunshine.utils.List> list) {
        this.list = list;
    }

    public WeatherData withList(java.util.List<com.tikalk.sunshine.utils.List> list) {
        this.list = list;
        return this;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public WeatherData withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }


}
