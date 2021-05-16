package com.andonova.netqa.models;

import com.google.gson.annotations.SerializedName;

public class JobObject {

    @SerializedName("date")
    String date;

    @SerializedName("host")
    String hostAddress;

    @SerializedName("count")
    int numPackets;

    @SerializedName("packetSize")
    int packetSize;

    @SerializedName("jobPeriod")
    int jobPeriod;                  //sec

    @SerializedName("jobType")
    String jobType;


    public JobObject(){};

    public JobObject(String date, String hostAddress, int numPackets, int packetSize, int jobPeriod, String jobType) {
        this.date = date;
        this.hostAddress = hostAddress;
        this.numPackets = numPackets;
        this.packetSize = packetSize;
        this.jobPeriod = jobPeriod;
        this.jobType = jobType;
    }

    @Override
    public String toString() {
        return "Job{" +
                "date=" + date +
                ", host=" + hostAddress +
                ", count='" + numPackets + '\'' +
                ", packetSize='" + packetSize + '\'' +
                ", jobPeriod='" + jobPeriod + '\'' +
                ", jobType=" + jobType + '\'' +
                '}';
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public int getNumPackets() {
        return numPackets;
    }

    public void setNumPackets(int numPackets) {
        this.numPackets = numPackets;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    public int getJobPeriod() {
        return jobPeriod;
    }

    public void setJobPeriod(int jobPeriod) {
        this.jobPeriod = jobPeriod;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
}
