package com.racetrack.model;

public class RunnerLogRow {

    private int id;
    private String date;
    private String workout;
    private String distance;
    private String time;
    private String pace;
    private String shoes;
    private String weather;
    private String feeling;
    private String sleep;
    private String hrAvg;
    private String hrMax;
    private String notes;

    public RunnerLogRow(int id, String date, String workout,
                        String distance, String time, String pace,
                        String shoes, String weather, String feeling,
                        String sleep, String hrAvg, String hrMax,
                        String notes) {

        this.id = id;
        this.date = date;
        this.workout = workout;
        this.distance = distance;
        this.time = time;
        this.pace = pace;
        this.shoes = shoes;
        this.weather = weather;
        this.feeling = feeling;
        this.sleep = sleep;
        this.hrAvg = hrAvg;
        this.hrMax = hrMax;
        this.notes = notes;
    }

    public int getId() { return id; }
    public String getDate() { return date; }
    public String getWorkout() { return workout; }
    public String getDistance() { return distance; }
    public String getTime() { return time; }
    public String getPace() { return pace; }
    public String getShoes() { return shoes; }
    public String getWeather() { return weather; }
    public String getFeeling() { return feeling; }
    public String getSleep() { return sleep; }
    public String getHrAvg() { return hrAvg; }
    public String getHrMax() { return hrMax; }
    public String getNotes() { return notes; }
}
