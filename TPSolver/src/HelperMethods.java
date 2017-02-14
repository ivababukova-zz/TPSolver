import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ivababukova on 12/17/16.
 */
public class HelperMethods {

    private HashMap<String, Airport> airports;
    private HashMap<Integer, Flight> flights;
    private HashMap<String, ArrayList<Integer>> instance;
    double T;

    public HelperMethods(HashMap<String, Airport> a, HashMap<Integer, Flight> f, double T, HashMap<String, ArrayList<Integer>> instance){
        this.airports = a;
        this.flights = f;
        this.T = T;
        this.instance = instance;
    }

    public Airport getHomePoint(){
        for (Airport a: this.airports.values()) {
            if(a.purpose.equals("home_point")) return a;
        }
        System.err.println("There is no specified home point");
        return null;
    }

    public Flight getFlightByID(int id){
        for (Flight f: this.flights.values()) {
            if (f.id == id) return f;
        }
        return null;
    }

//    // this containts trip property 4
//    public ArrayList<Integer> allToAirport(Airport a) {
//        ArrayList<Integer> toa = new ArrayList<>();
//        for (Flight f: flights.values()) {
//            double time = f.date + f.duration;
//            if (f.arr == a && time <= T) {
//                toa.add(f.id);
//            }
//        }
//        return toa;
//    }

    public int[] allowedNextFlightsHC1(int prev, double lb, double up) {
        ArrayList<Integer> froma = new ArrayList<>();
        Flight f = getFlightByID(prev);
        Airport dep = f.arr;
        for (Flight fl: flights.values()) {
            if (fl.dep == dep && fl.date > f.date) {
                double stay_time = fl.date - (f.date + f.duration + dep.conn_time);
                if (stay_time >= lb && stay_time <= up) {
                    froma.add(fl.id);
                }
            }
        }
        return arrayToint(froma);
    }

    public ArrayList<Integer> allToBefore(Airport a, double date) {
        ArrayList<Integer> toa = new ArrayList<>();
        for (Flight f: flights.values()) {
            if (f.arr == a && (f.date + f.duration + a.conn_time) <= date) {
                toa.add(f.id);
            }
        }
        return toa;
    }

    public ArrayList<Integer> allFromAfter(Airport a, double date) {
        ArrayList<Integer> froma = new ArrayList<>();
        for (Flight f: flights.values()) {
            // allow for at least 1 day stay at a. Date is multiplied by 0 due to
            // choco not supporting double values properly
            if (f.dep == a && f.date >= date + 10) {
                froma.add(f.id);
            }
        }
        return froma;
    }

    public ArrayList<Integer> allowedNextFlights(Flight fl) {
        ArrayList<Integer> allowedFlights = new ArrayList<>();
        Airport a = fl.arr;
        double connTime = a.conn_time;
        for (Flight f: flights.values()) {
            double time = fl.date + fl.duration + connTime;
            if (f.dep == a && time <= f.date) {
                allowedFlights.add(f.id);
            }
        }
        return allowedFlights;
    }

    public ArrayList<Integer> allowedLastFlights(Flight fl) {
        ArrayList<Integer> allowedFlights = new ArrayList<>();
        Airport a = fl.arr;
        for (Flight f: flights.values()) {
            double time = fl.date + fl.duration;
            if (f.dep == a && time <= f.date){
                allowedFlights.add(f.id);
            }
        }
        return allowedFlights;
    }

    // this is for trip properties 3,4 for the IP model
    public ArrayList<Integer> disallowedPrev(int next_id) {
        ArrayList<Integer> forbidden = new ArrayList<>();
        Flight next = getFlightByID(next_id);
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
        Airport a0 = getHomePoint();
        if (f.arr == a0 && f.dep == a0 && f.duration == 0 && f.cost == 0) {
            return 0;
        }
        return f.dep.conn_time;
    }

    public int[] arrayToint(ArrayList<Integer> arr){
        int[] array = new int[arr.size()];
        for(int k = 0; k < arr.size(); k++){
            array[k] = arr.get(k);
        }
        return array;
    }

    public ArrayList<Airport> getDestinations(){
        ArrayList<Airport> destinations = new ArrayList<>();
        for (Airport a: this.airports.values()) {
            if(a.purpose.equals("destination")) {
                destinations.add(a);
            }
        }
        return destinations;
    }

    public ArrayList<Integer> allConnectionFlights(){
        ArrayList<Integer> all = new ArrayList<>();
        for (Flight f : flights.values()) {
            if (f.arr.purpose.equals("connecting")) {
                all.add(f.id);
            }
        }
        return all;
    }

    public Flight getCheapestAfter(float cost){
        double cheapest = 99999;
        Flight cheapestF = null;
        for (Flight f: flights.values()) {
            if(f.cost > cost && f.cost < cheapest) {
                cheapest = f.cost;
                cheapestF = f;
            }
        }
        return cheapestF;
    }
}
