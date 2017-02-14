/**
 * Created by ivababukova on 12/16/16.
 */
public class Airport {

    public String name;
    public double conn_time;
    public String purpose;
    public int index; // this is for hard constraint 2

    public Airport(String name, double t, String p){
        this.name = name;
        this.conn_time = t;
        this.purpose = p.trim();
        this.index = 0;
    }

    public void setIndex(int i) {
        this.index = i;
    }

}
