/**
 * Created by ivababukova on 1/26/17.
 */

public class Triplet {

    private Airport a;
    private double lb;
    private double ub;
    public Triplet(Airport x, double lb, double ub) {
        this.a = x;
        this.lb = lb;
        this.ub = ub;
    }

    public Airport getA(){
        return this.a;
    }

    public double getLb(){
        return this.lb;
    }

    public double getUb(){
        return this.ub;
    }

}
