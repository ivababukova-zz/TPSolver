package main;

/**
 * Created by ivababukova on 12/16/16.
 */
public class Airport {

    public String name;
    public float conn_time;
    public int purpose; // 0 for home point, 1 for destination, 2 for connection airport
    public int index; // this is for hard constraint 2

    public Airport(String name, float t, int p){
        this.name = name;
        this.conn_time = t;
        this.purpose = p;
        this.index = 0;
    }

    public void setIndex(int i) {
        this.index = i;
    }

}
