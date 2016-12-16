package main;

/**
 * Created by ivababukova on 12/16/16.
 */
public class Airport {

    String name;
    float conn_time;
    int purpose; // 0 for home point, 1 for destination, 2 for connection airport

    public Airport(String name, float t, int p){
        this.name = name;
        this.conn_time = t;
        this.purpose = p;
    }

}
