package common;

import java.util.ArrayList;

/**
 * Created by ivababukova on 12/17/16.
 */
public class HelperMethods {

    private ArrayList<Airport> airports;
    private ArrayList<Flight> flights;

    public HelperMethods(ArrayList<Airport> a, ArrayList<Flight> f){
        this.airports = a;
        this.flights = f;
    }

    public  Airport getAirportByName(String name){
        for (Airport a : this.airports) {
            if (a.name.equals(name)) return a;
        }
        return null;
    }

    public Airport getHomePoint(){
        for (Airport a: this.airports) {
            if(a.purpose.equals("home_point")) return a;
        }
        System.err.println("There is no specified home point");
        return null;
    }

    public Flight getFlightByID(int id){
        for (Flight f: this.flights) {
            if (f.id == id) return f;
        }
        return null;
    }

    public ArrayList<Integer> allTo(Airport a) {
        ArrayList<Integer> toa = new ArrayList<>();
        for (Flight f: flights) {
            if (f.arr == a) {
                toa.add(f.id);
            }
        }
        return toa;
    }

    public ArrayList<Integer> allToBefore(Airport a, double date) {
        ArrayList<Integer> toa = new ArrayList<>();
        for (Flight f: flights) {
            if (f.arr == a && (f.date + f.duration + a.conn_time) <= date) {
                toa.add(f.id);
            }
        }
        return toa;
    }

    // this containts trip property 4
    public ArrayList<Integer> allToHome(Airport a, float T) {
        ArrayList<Integer> toa = new ArrayList<>();
        for (Flight f: flights) {
            double time = f.date + f.duration;
            if (f.arr == a && time <= T) {
                toa.add(f.id);
            }
        }
        return toa;
    }

    public ArrayList<Integer> allFrom(Airport a) {
        ArrayList<Integer> froma = new ArrayList<>();
        for (Flight f: flights) {
            if (f.dep == a) {
                froma.add(f.id);
            }
        }
        return froma;
    }

    public ArrayList<Integer> allFromAfter(Airport a, double date) {
        ArrayList<Integer> froma = new ArrayList<>();
        Flight test = this.getFlightByID(7);
        for (Flight f: flights) {
            if (f.dep == a && f.date >= date + 1) { // allow for at least 1 day stay at a
                froma.add(f.id);
            }
        }
        return froma;
    }

    public ArrayList<Integer> allFromTimedConn(Flight fl) {
        ArrayList<Integer> allowedFlights = new ArrayList<>();
        Airport a = fl.arr;
        double connTime = a.conn_time;
        for (Flight f: flights) {
            double time = fl.date + fl.duration + connTime;
            if (f.dep == a && time <= f.date) {
                allowedFlights.add(f.id);
            }
        }
        return allowedFlights;
    }

    public ArrayList<Integer> allFromTimed(Flight fl) {
        ArrayList<Integer> allowedFlights = new ArrayList<>();
        Airport a = fl.arr;
        for (Flight f: flights) {
            double time = fl.date + fl.duration;
            if (f.dep == a && time <= f.date){
                allowedFlights.add(f.id);
            }
        }
        return allowedFlights;
    }

    // this is for trip properties 3,4 for the IP model
    public ArrayList<Integer> disallowedPrev(int next_id) {
        ArrayList<Integer> toa = new ArrayList<>();
        Flight next = getFlightByID(next_id);
        double conn_time = getConnTimeIP(next);
        for (Flight prev : flights) {
            double arrival_prev = prev.date + prev.duration;
            if (arrival_prev + conn_time > next.date) {
                toa.add(prev.id);
            }
        }
        return toa;
    }

    // this is for trip properties 3,4 for the IP model
    public double getConnTimeIP(Flight f) {
        Airport a0 = getHomePoint();
        if (f.arr == a0 && f.dep == a0 && f.duration == 0 && f.cost == 0) {
            return 0;
        }
        return f.arr.conn_time;
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
        for (Airport a: this.airports) {
            if(a.purpose.equals("destination")) {
                destinations.add(a);
            }
        }
        return destinations;
    }

    public Flight getCheapestAfter(float cost){
        double cheapest = 99999;
        Flight cheapestF = null;
        for (Flight f: flights) {
            if(f.cost > cost && f.cost < cheapest) {
                cheapest = f.cost;
                cheapestF = f;
            }
        }
        return cheapestF;
    }

}
