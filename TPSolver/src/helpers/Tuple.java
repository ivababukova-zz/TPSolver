package helpers;

import main.Airport;

/**
 * Created by ivababukova on 1/5/17.
 */
public class Tuple {

    private Airport a;
    private float date;
    public Tuple(Airport x, float y) {
        this.a = x;
        this.date = y;
    }

    public Airport getA(){
        return this.a;
    }

    public float getDate(){
        return this.date;
    }
}