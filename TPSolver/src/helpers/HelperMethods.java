package helpers;

import main.Airport;
import main.Flight;

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
            if(a.purpose == 0) return a;
        }
        System.err.println("There is no specified ");
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

    public ArrayList<Integer> allFrom(Airport a) {
        ArrayList<Integer> froma = new ArrayList<>();
        for (Flight f: flights) {
            if (f.dep == a) {
                froma.add(f.id);
            }
        }
        return froma;
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
            if(a.purpose == 1) {
                destinations.add(a);
            }
        }
        return destinations;
    }
}