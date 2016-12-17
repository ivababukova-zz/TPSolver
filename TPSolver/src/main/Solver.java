package main;

import java.util.ArrayList;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.*;
import org.chocosolver.solver.constraints.*;

/**
 * Created by ivababukova on 12/16/16.
 * this is the CP solver for TP
 */
public class Solver {

    ArrayList<Airport> airports;
    ArrayList<Flight> flights;
    Model model;
    IntVar[] S;
    IntVar z;
    int T;

    public Solver(ArrayList<Airport> as, ArrayList<Flight> fs, int T){
        this.airports = as;
        this.flights = fs;
        this.T = T;
    }

    private void init(){
        this.model = new Model("TP Solver");
        this.S = model.intVarArray("Flights Schedule", flights.size() + 1, 0, flights.size());
        this.z = model.intVar("End of schedule", 2, flights.size());

        Airport a0 = getHomePoint();
        int[] to_home = arrayToint(allTo(a0));
        int[] from_home = arrayToint(allFrom(a0));

        model.member(S[0], from_home).post();

        for(int i = 1; i <= flights.size(); i++) {
            Flight f = getFlightByID(i);
            departureConstraint(f);

            // if the sequence ends at i, then s[i] must be 0
            model.ifThen(
                    model.arithm(z, "=", i),
                    model.and(model.arithm(S[i], "=", 0), model.arithm(S[i-1], "!=", 0))
            );

            // if s[i-1] is 0, then s[i] must be 0
            model.ifThen(
                    model.arithm(S[i-1], "=", 0),
                    model.arithm(S[i], "=", 0)
            );

            // if s[i] is not 0, then s[i-1] must not be 0
            model.ifThen(
                    model.arithm(S[i], "!=", 0),
                    model.arithm(S[i-1], "!=", 0)
            );

            for (int zet = 2; zet <= flights.size(); zet++) {
                model.ifThen(
                        model.arithm(z, "=", zet),
                        model.member(S[zet-1], to_home)
                );
            }
        }
        this.model.allDifferentExcept0(S).post();

        // todo the dep and arr airports of two consecutive flights must be the same:

        // todo the duration + time + conn time constraint

        // todo the total time of the schedule must be less or equal to T

    }

    // for every S[i], if S[i] = j, then S[i+1] must depart from the arrival airport of j
    private void departureConstraint(Flight f){
        ArrayList<Integer> af = allFrom(f.arr);
        af.add(0);
        int[] all_from = arrayToint(af);
        for (int j = 1; j <= flights.size(); j++) {
            model.ifThen(
                    model.arithm(S[j-1],"=", f.id),
                    model.member(S[j], all_from)
            );
        }
    }

    public void solve(){
        init();
        //model.getSolver().propagate();
        model.getSolver().solve();
//        while (model.getSolver().solve()) {
//            for (IntVar s : S) {
//                System.out.print(s.getValue() + " ");
//            }
//            System.out.println();
//        }
        for (IntVar s : S) {
            System.out.print(s.getValue() + " ");
        }
        System.out.print("\nAnd z is: ");
        System.out.println(z.getValue());
    }

    private  Airport getAirportByName(String name){
        for (Airport a : this.airports) {
            if (a.name.equals(name)) return a;
        }
        return null;
    }

    private Airport getHomePoint(){
        for (Airport a: this.airports) {
            if(a.purpose == 0) return a;
        }
        System.err.println("There is no specified ");
        return null;
    }

    private Flight getFlightByID(int id){
        for (Flight f: this.flights) {
            if (f.id == id) return f;
        }
        return null;
    }

    private ArrayList<Integer> allTo(Airport a) {
        ArrayList<Integer> toa = new ArrayList<>();
        for (Flight f: flights) {
            if (f.arr == a) {
                toa.add(f.id);
            }
        }
        return toa;
    }

    private ArrayList<Integer> allFrom(Airport a) {
        ArrayList<Integer> froma = new ArrayList<>();
        for (Flight f: flights) {
            if (f.dep == a) {
                froma.add(f.id);
            }
        }
        return froma;
    }

    private int[] arrayToint(ArrayList<Integer> arr){
        int[] array = new int[arr.size()];
        for(int k = 0; k < arr.size(); k++){
            array[k] = arr.get(k);
        }
        return array;
    }

}
