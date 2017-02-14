import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ivababukova on 12/17/16.
 */
public class HelperMethods {

    private HashMap<Integer, Flight> flights;
    HashMap<String, ArrayList<Integer>> depFlights;
    HashMap<String, ArrayList<Integer>> arrFlights;
    ArrayList<Airport> destinations;
    ArrayList<Airport> connecting;
    Airport a0;

    public HelperMethods(
            HashMap<String, Airport> a,
            HashMap<Integer, Flight> f,
            HashMap<String,ArrayList<Integer>> dep,
            HashMap<String, ArrayList<Integer>> arr
    ) {
        this.flights = f;
        this.connecting = new ArrayList<>();
        this.destinations = new ArrayList<>();
        for (Airport a1 : a.values()) {
            if (a1.purpose.equals("home_point")) {
                a0 = a1;
            }
            else if (a1.purpose.equals("connecting")) {
                connecting.add(a1);
            }
            if (a1.purpose.equals("destination")) {
                destinations.add(a1);
            }
        }

        this.depFlights = dep;
        this.arrFlights = arr;
    }

    public ArrayList<Integer> allToBefore(Airport a, double date) {
        ArrayList<Integer> toa = new ArrayList<>();
        for (int j : arrFlights.get(a.name)) {
            Flight f = flights.get(j);
            if ((f.date + f.duration + a.conn_time) <= date) {
                toa.add(f.id);
            }
        }
        return toa;
    }

    public ArrayList<Integer> allFromAfter(Airport a, double date) {
        ArrayList<Integer> froma = new ArrayList<>();
        for (int j : depFlights.get(a.name)) {
            Flight f = flights.get(j);
            // allow for at least 1 day stay at a. Date is multiplied by 100 due to
            // choco not supporting double values properly
            if (f.date >= date + 100) {
                froma.add(f.id);
            }
        }
        return froma;
    }

    public ArrayList<Integer> allowedNext(Flight fl, double connTime) {
        ArrayList<Integer> allowedFlights = new ArrayList<>();
        for (int j : depFlights.get(fl.arr.name)) {
            Flight f = flights.get(j);
            double time = fl.date + fl.duration + connTime;
            if (time <= f.date) {
                allowedFlights.add(f.id);
            }
        }
        return allowedFlights;
    }

    public ArrayList<Integer> allowedNextHC1(Flight f, double lb, double up) {
        ArrayList<Integer> froma = new ArrayList<>();
        for (int j : depFlights.get(f.arr.name)) {
            Flight fl = flights.get(j);
            if (fl.date > f.date) {
                double stay_time = fl.date - (f.date + f.duration + f.arr.conn_time);
                if (stay_time >= lb && stay_time <= up) {
                    froma.add(fl.id);
                }
            }
        }
        return froma;
    }

    // this is for trip properties 3,4 for the IP model
    public ArrayList<Integer> disallowedPrev(int next_id) {
        ArrayList<Integer> forbidden = new ArrayList<>();
        Flight next = flights.get(next_id);
        double conn_time = getConnTimeIP(next);
        for (Flight prev : flights.values()) {
            double arrival_prev = prev.date + prev.duration;
            if (arrival_prev + conn_time > next.date) {
                forbidden.add(prev.id);
            }
        }
        return forbidden;
    }

    // this is for trip properties 3,4 for the IP model
    public double getConnTimeIP(Flight f) {
        if (f.arr == a0 && f.dep == a0 && f.duration == 0 && f.cost == 0) {
            return 0;
        }
        return f.dep.conn_time;
    }

    public int[] arrayToint(ArrayList<Integer> arr) {
        int[] array = new int[arr.size()];
        for (int k = 0; k < arr.size(); k++) {
            array[k] = arr.get(k);
        }
        return array;
    }

    public int[] intersection(ArrayList<Integer> one, ArrayList<Integer> two){
        int[] three = new int[one.size()];
        int len = 0;
        for (int i : one) {
            if (two.contains(i)) {
                three[len] = i;
                len += 1;
            }
        }
        return three;
    }
}
