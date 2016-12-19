package main;

import java.util.ArrayList;

import helpers.HelperMethods;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.*;

/**
 * Created by ivababukova on 12/16/16.
 * this is the CP solver for TP
 */
public class Solver {

    private ArrayList<Airport> airports;
    private ArrayList<Flight> flights;
    private HelperMethods h;
    private int T;
    private int B; // upper bound on the cost

    private Model model;
    private IntVar[] S;
    private IntVar z;
    private IntVar[] C;

    public Solver(ArrayList<Airport> as, ArrayList<Flight> fs, int T, int B){
        this.airports = as;
        this.flights = fs;
        this.T = T;
        this.h = new HelperMethods(as, fs);
    }

    // filters out flights that won't arrive within the specified travel time
    private void filtering() {
        for (Flight f: this.flights) {
            if (f.date + f.duration > this.T) {
                this.flights.remove(f);
            }
        }
    }

    private void init(){
//        filtering();

        this.model = new Model("TP Solver");
        this.S = model.intVarArray("Flights Schedule", flights.size() + 1, 0, flights.size());
        this.z = model.intVar("End of schedule", 2, flights.size());
        this.C = this.model.intVarArray("The cost of each taken flight", flights.size() + 1, 0, 5555); // todo instead of B as upper bound, make upper bound = most expensive flight

        Airport a0 = h.getHomePoint();
        int[] to_home = h.arrayToint(h.allToHome(a0, this.T));
        int[] from_home = h.arrayToint(h.allFrom(a0));

        model.member(S[0], from_home).post();

        for(int i = 1; i <= flights.size(); i++) {
            Flight f = h.getFlightByID(i);

            timeConstraint(f);
//
            this.model.ifThen(
                    model.arithm(S[i], "=", 0),
                    model.arithm(C[i], "=", 0)
            );
//
//            this.model.ifThen(
//                    model.arithm(S[i], "!=", 0),
//                    model.arithm(C[i], "=", 99)
//            );

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

            // the last non-zero flight should arrive at the home point
            model.ifThen(
                    model.arithm(z, "=", i),
                    model.member(S[i-1], to_home)
            );

//            costConstraint(i); // todo this is wrong
        }
        this.model.allDifferentExcept0(S).post();

        // all destinations must be visited
        for (Airport d: h.getDestinations()) {
            int[] all_to = h.arrayToint(h.allTo(d));
            IntVar X = this.model.intVar(1, all_to.length);
            this.model.among(X, S, all_to).post();
        }

        // todo when the instance has no solutions, we need to get an error

        costConstraint();

    }

    private void costConstraint(){
        for(int i = 1; i<=flights.size(); i++) {
            int cost = Math.round(h.getFlightByID(i).cost);
            for (int j = 0; j <= flights.size(); j++) {
                this.model.ifThen(
                        model.arithm(S[j], "=", i),
                        model.arithm(C[j], "=", cost)
                );
            }
        }
    }

    // for every S[i], if S[i] = j, then S[i+1] must depart from the arrival airport of j
    private void departureConstraint(Flight f){
        ArrayList<Integer> af = h.allFrom(f.arr);
        af.add(0);
        int[] all_from = h.arrayToint(af);
        for (int j = 1; j <= flights.size(); j++) {
            model.ifThen(
                    model.arithm(S[j-1],"=", f.id),
                    model.member(S[j], all_from)
            );
        }
    }

    // enforces both time and departure constraints at the same time for all flights but the last flight
    private void timeConstraint(Flight f){
        ArrayList<Integer> af = h.allFromTimed(f.arr, f, f.arr.conn_time); // todo: if the flight is last, 0 conn time
        af.add(0);
        int[] all_from = h.arrayToint(af);
        for (int j = 1; j <= flights.size(); j++) {
            model.ifThen(
                    model.and(model.arithm(S[j-1],"=", f.id), model.arithm(z, "!=", j)),
                    model.member(S[j], all_from)
            );
        }
    }

    private void min_cost(){
        IntVar x = this.model.intVar(0,B);
        this.model.sum(C, "<=", x).post();
        this.model.setObjective(Model.MINIMIZE, x);
    }

    public void solve(){
        init();
        //model.getSolver().propagate();
        model.getSolver().solve();
//        while (model.getSolver().solve()) {
//            for (IntVar s : S) {
//                if(s.getValue() != 0){
//                    System.out.print(h.getFlightByID(
//                            s.getValue()).dep.name +
//                            "" +
//                            h.getFlightByID(s.getValue()).arr.name +
//                            ", "
//                    );
//                }
//                else{
//                    System.out.print(s.getValue() + ", ");
//                }
//            }
//            System.out.println(z.getValue());
//        }
        for (IntVar s : S) {
            if(s.getValue() != 0){
                System.out.print(h.getFlightByID(
                        s.getValue()).dep.name +
                        "" +
                        h.getFlightByID(s.getValue()).arr.name +
                        ", "
                );
            }
            else{
                System.out.print(s.getValue() + ", ");
            }
        }
        System.out.println();
        for (IntVar s : C) {
                System.out.print(s.getValue() + ", ");
        }
        System.out.print("\nAnd z is: ");
        System.out.println(z.getValue());
    }
}
