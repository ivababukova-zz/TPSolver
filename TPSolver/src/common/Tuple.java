package common;

import common.Airport;

/**
 * Created by ivababukova on 1/5/17.
 */
public class Tuple {

    private Airport a;
    private double date;
    public Tuple(Airport x, double y) {
        this.a = x;
        this.date = y;
    }

    public Airport getA(){
        return this.a;
    }

    public double getDate(){
        return this.date;
    }
}