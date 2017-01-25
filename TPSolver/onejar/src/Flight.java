/**
 * Created by ivababukova on 12/16/16.
 */
public class Flight {

    public int id;
    public Airport dep;
    public Airport arr;
    public double date;
    public double duration;
    public double cost;

    public Flight(int id,
                  Airport dep,
                  Airport arr,
                  double date,
                  double duration,
                  double cost){
        this.id = id;
        this.dep = dep;
        this.arr = arr;
        this.date = date;
        this.duration = duration;
        this.cost = cost;
    }

}
