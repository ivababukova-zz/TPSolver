/**
 * Created by ivababukova on 1/26/17.
 */

public class Triplet {

    private Airport a;
    private int lb;
    private int ub;
    public Triplet(Airport x, int lb, int ub) {
        this.a = x;
        this.lb = lb;
        this.ub = ub;
    }

    public Airport getA(){
        return this.a;
    }

    public int getLb(){
        return this.lb;
    }

    public int getUb(){
        return this.ub;
    }

}
