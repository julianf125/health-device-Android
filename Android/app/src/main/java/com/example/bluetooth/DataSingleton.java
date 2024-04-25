package com.example.bluetooth;

public class DataSingleton {
    private static DataSingleton instance = null;

    // Weather data
    private double temperature, pressure, humidity, gas;

    // Medical data
    private double heartRate, bloodOxygen, bodyTemperature;

    // Private constructor to prevent instantiation
    private DataSingleton() {
    }

    // Static method to get the instance
    public static DataSingleton getInstance() {
        if (instance == null) {
            instance = new DataSingleton();
        }
        return instance;
    }

    // Methods to set data
    public void setBodyTemp(double data) {
        this.bodyTemperature = data;
    }
    public void setBloodOx(double data) {
        this.bloodOxygen = data;
    }
    public void setHeartRate(double data) {
        this.heartRate = data;
    }
    public void setTempData(double data) {
        this.temperature = data;
    }
    public void setPressData(double data) {
        this.pressure = data;
    }
    public void setHumidity(double data) {
        this.humidity = data;
    }
    public void setGas(double data) {
        this.gas = data;
    }


    // Methods to get data
    public double getHeartRate() {
        return heartRate;
    }
    public double getBloodOx() { return bloodOxygen; }
    public double getBodyTemp() { return bodyTemperature; }
    public double getTempData() { return temperature; }
    public double getPressData() { return pressure; }
    public double getHumidity() { return humidity; }
    public double getGas() { return gas; }
}
