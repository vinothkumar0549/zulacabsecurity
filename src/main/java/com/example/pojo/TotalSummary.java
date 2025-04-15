package com.example.pojo;

public class TotalSummary {

    private int userid;
    private int trips;
    private int fare;
    private int commission;

    public TotalSummary(int userid, int trips, int fare, int commission) {
        this.userid = userid;
        this.trips = trips;
        this.fare = fare;
        this.commission = commission;
    }

    public TotalSummary(int userid, int trips, int fare) {
        this.userid = userid;
        this.trips = trips;
        this.fare = fare;
    }

    
}
