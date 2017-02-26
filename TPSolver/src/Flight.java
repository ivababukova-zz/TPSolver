/**
 * Created by ivababukova on 12/16/16.
 */
public class Flight {

    public int id;
    public Airport dep;
    public Airport arr;
    public int date;
    public int duration;
    public int cost;

    public Flight(int id,
                  Airport dep,
                  Airport arr,
                  int date,
                  int duration,
                  int cost){
        this.id = id;
        this.dep = dep;
        this.arr = arr;
        this.date = date;
        this.duration = duration;
        this.cost = cost;
    }

}
