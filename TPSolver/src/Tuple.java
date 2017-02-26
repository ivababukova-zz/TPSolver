
/**
 * Created by ivababukova on 1/5/17.
 */
public class Tuple {

    private Airport a;
    private int date;
    public Tuple(Airport x, int y) {
        this.a = x;
        this.date = y;
    }

    public Airport getA(){
        return this.a;
    }

    public int getDate(){
        return this.date;
    }
}