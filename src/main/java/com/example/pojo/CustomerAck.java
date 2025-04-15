package com.example.pojo;

public class CustomerAck {

    private int cabid;
    private int distance;
    private int fare;
    private String source;
    private String destiantion;

    public CustomerAck(int cabid, int distance, int fare, String source, String destination) {
        this.cabid = cabid;
        this.distance = distance;
        this.fare = fare;
        this.source = source;
        this.destiantion = destination;
    }

}
