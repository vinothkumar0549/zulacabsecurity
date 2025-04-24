package com.example.pojo;

import java.sql.Date;
import com.fasterxml.jackson.annotation.JsonFormat;


public class Penalty {
    
    private int customerid;
    private int penalty;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date date;

    public Penalty() {

    }

    public Penalty(int penalty, Date date) {
        this.penalty = penalty;
        this.date = date;
    }
    
    public Penalty(int customerid, int penalty, Date date) {
        this.customerid = customerid;
        this.penalty = penalty;
        this.date = date;
    }

    public int getCustomerid() {
        return customerid;
    }

    public void setCustomerid(int customerid) {
        this.customerid = customerid;
    }

    public int getPenalty() {
        return penalty;
    }

    public void setPenalty(int penalty) {
        this.penalty = penalty;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(customerid).append(", ");
        sb.append(penalty).append(", ");
        sb.append(date);
        return sb.toString();
    }

    
}
