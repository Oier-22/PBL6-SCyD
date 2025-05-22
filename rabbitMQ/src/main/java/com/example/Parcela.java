package com.example;

import java.io.Serializable;

public class Parcela implements Serializable {
    private String id;
    private double temperatura;
    private double humedad;
    private double viento;
    private double radiacion;
    private double precipitacion;
    private String tipoDePlanta;
    private String etapaCrecimiento;
    private double humedadSuelo;
    private int diaDelAnio;
    private double consumoAgua;

    public Parcela(String id, double temperatura, double humedad, double viento, double radiacion, double precipitacion, 
                   String tipoDePlanta, String etapaCrecimiento, double humedadSuelo, int diaDelAnio) {
        this.id = id;
        this.temperatura = temperatura;
        this.humedad = humedad;
        this.viento = viento;
        this.radiacion = radiacion;
        this.precipitacion = precipitacion;
        this.tipoDePlanta = tipoDePlanta;
        this.etapaCrecimiento = etapaCrecimiento;
        this.humedadSuelo = humedadSuelo;
        this.diaDelAnio = diaDelAnio;
    }

    @Override
    public String toString() {
        return "Parcela{id='" + id + "', temperatura=" + temperatura + ", humedad=" + humedad + 
               ", viento=" + viento + ", radiacion=" + radiacion + ", precipitacion=" + precipitacion + 
               ", tipoDePlanta='" + tipoDePlanta + "', etapaCrecimiento='" + etapaCrecimiento + 
               "', humedadSuelo=" + humedadSuelo + ", diaDelAnio=" + diaDelAnio + '}';
    }

    // Getters y setters
    public String getId() { return id; }
    public double getTemperatura() { return temperatura; }
    public double getHumedad() { return humedad; }
    public double getViento() { return viento; }
    public double getRadiacion() { return radiacion; }
    public double getPrecipitacion() { return precipitacion; }
    public String getTipoDePlanta() { return tipoDePlanta; }
    public String getEtapaCrecimiento() { return etapaCrecimiento; }
    public double getHumedadSuelo() { return humedadSuelo; }
    public int getDiaDelAnio() { return diaDelAnio; }
}