package com.openatlas.homelauncher.otto;

/**
 * Created by BunnyBlue on 8/31/15.
 */
public class OttoMessage {
  public   long  time;


    public  String where;
    public  String message;
    public OttoMessage(String message, String where, long time) {
        this.message = message;
        this.where = where;
        this.time = time;
    }
    @Override
    public String toString() {
        return "OttoMessage{" +
                "time=" + time +
                ", where='" + where + '\'' +
                ", message='" + message + '\'' +
                '}';
    }


}
