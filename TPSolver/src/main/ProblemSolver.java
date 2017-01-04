package main;

import java.util.ArrayList;
import java.util.Iterator;

import helpers.HelperMethods;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.*;
import org.chocosolver.solver.variables.*;

/**
 * Created by ivababukova on 12/16/16.
 * this is the CP solver for TP
 */
public class ProblemSolver {

    private ArrayList<Airport> airports;
    private ArrayList<Flight> flights;
    private HelperMethods h;
    private int T;
    private int B; // upper bound on the cost
    private String[] args;

    private Model model;
    private Solver solver;
    private IntVar[] S;
    private IntVar z;
    private IntVar[] C;
    private IntVar cost_sum;

    public ProblemSolver(ArrayList<Airport> as, ArrayList<Flight> fs, int T, int B, String[] args){
        this.airports = as;
        this.flights = fs;
        this.T = T;
        this.B = B;
        this.h = new HelperMethods(as, fs);
        this.args = args;
    }

    // filters out flights that won't arrive within the specified travel time
    private void filtering() {
        ArrayList<Flight> newflights = new ArrayList<>();
        for(Flight f: this.flights) {
            if (f.date + f.duration <= this.T) newflights.add(f);
        }
        this.flights = newflights;
    }

    private void init(){
        this.model = new Model("TP ProblemSolver");
        this.S = model.intVarArray("Flights Schedule", flights.size() + 1, 0, flights.size());
        this.z = model.intVar("End of schedule", 2, flights.size());
        this.C = this.model.intVarArray("The cost of each taken flight", flights.size() + 1, 0, 500);
        this.cost_sum = this.model.intVar(0, B);
        this.model.sum(C, "=", cost_sum).post();
        this.solver = model.getSolver();

        this.findSchedule();

    }

    private void findSchedule() {
        Airport a0 = h.getHomePoint();
        int[] to_home = h.arrayToint(h.allToHome(a0, this.T));
        int[] from_home = h.arrayToint(h.allFrom(a0));

        model.member(S[0], from_home).post();
        this.model.arithm(S[1], "!=", 0).post();
        this.model.arithm(C[0], "!=", 0).post();

        destinationConstraint();

        for(int i = 1; i <= flights.size(); i++) {
            Flight f = h.getFlightByID(i);

            timeConstraint(f);

            // if the sequence ends at i, then s[i] must be 0
            model.ifThen(
                    model.arithm(z, "=", i),
                    model.and(
                            model.arithm(S[i], "=", 0),
                            model.member(S[i-1], to_home)
                    )
            );

            model.ifThen(
                    model.arithm(z, ">", i),
                    model.arithm(S[i], "!=", 0)
            );

            // if s[i-1] is 0, then s[i] must be 0
            model.ifThen(
                    model.arithm(S[i-1], "=", 0),
                    model.arithm(S[i], "=", 0)
            );

            this.model.ifThen(
                    model.arithm(S[i], "=", 0),
                    model.arithm(C[i], "=", 0)
            );

            this.model.ifThen(
                    model.arithm(S[i], "!=", 0),
                    model.arithm(C[i], "!=", 0)
            );

            // if s[i] is not 0, then s[i-1] must not be 0
            model.ifThen(
                    model.arithm(S[i], "!=", 0),
                    model.arithm(S[i-1], "!=", 0)
            );
        }
        costConstraint();
        this.model.allDifferentExcept0(S).post();
        this.dateLocationConstraint(airports.get(3), 3);

    }

    // call this function when no flights to connection airports are allowed
    private void removeConnections(){
        ArrayList<Flight> newflights = new ArrayList<>();
        for(Flight f: this.flights) {
            if (f.arr.purpose != 2 && f.dep.purpose != 2) newflights.add(f);
        }
        this.flights = newflights;
        for (Flight f: newflights) {
            System.out.print(f.dep.name + f.arr.name + " ");
        }
        System.out.println();
    }

    // hard constraint 2
    // be at date d at airport a
    // arrive at a before d. The next flight should leave a at date after d
    private void dateLocationConstraint(Airport a, float date) {
        int[] all_to_before = h.arrayToint(h.allToBefore(a, date)); // all flights to desired destination
        int[] all_from_after = h.arrayToint(h.allFromAfter(a, date)); // all flights from desired destination
        IntVar X = this.model.intVar(1, all_to_before.length);
        IntVar Y = this.model.intVar(1, all_from_after.length);
        this.model.among(X, S, all_to_before).post(); // S must contain at least one flight that goes to d
        this.model.among(Y, S, all_from_after).post();

        for(int i = 1; i <= flights.size(); i++) {
            for (int j : all_to_before) {
                model.ifThen(
                        model.arithm(S[i-1], "=", j),
                        model.member(S[i], all_from_after)
                );
            }
        }
        
        System.out.print("All flights before ");
        for (int j : all_to_before) {
            System.out.print(j + " ");
        }
        System.out.print("\nAll flights after ");
        for (int j : all_from_after) {
            System.out.print(j + " ");
        }
        System.out.println();
    }

    // all destinations must be visited
    private void destinationConstraint(){
        for (Airport d: h.getDestinations()) {
            int[] all_to = h.arrayToint(h.allTo(d)); // all flights that fly to d
            IntVar X = this.model.intVar(1, all_to.length);
            this.model.among(X, S, all_to).post(); // S must contain at least one flight that goes to d
        }
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

    // enforces both time and departure constraints at the same time for all flights but the last flight
    private void timeConstraint(Flight f){
        ArrayList<Integer> af = h.allFromTimed(f.arr, f, f.arr.conn_time);
        int[] all_from = h.arrayToint(af);
        for (int j = 1; j <= flights.size(); j++) {
            model.ifThen(
                    model.and(model.arithm(S[j-1],"=", f.id), model.arithm(z, "!=", j)),
                    model.member(S[j], all_from)
            );
        }
    }

    public void getSolution(){
        init();
        Solution x;
        Boolean m = null;
        IntVar to_optimise = null;

        if (args.length == 0) {
            solver.solve();
            printSolution();
            return;
        }

        if (args.length == 2){

            if (args[0].equals("-min")) {
                System.out.print("Solution with minimum ");
                m = Model.MINIMIZE;
            } else if (args[0].equals("-max")) {
                System.out.print("Solution with maximum ");
                m = Model.MAXIMIZE;
            } else {
                System.out.println("Wrong first argument provided");
                return;
            }

            if (args[1].equals("-cost")) {
                System.out.println("cost:");
                to_optimise = this.cost_sum;
            } else if (args[1].equals("-flights")) {
                System.out.println("number of flights:");
                to_optimise = this.z;
            } else {
                System.out.println("Wrong second argument provided");
                return;
            }

            x = solver.findOptimalSolution(to_optimise, m);
            printOptSolution(x);
        }

        else {
            System.out.println("Not enough arguments provided");
        }
    }

    private void printSolution(){
        for (IntVar s1 : S) {
            if(s1.getValue() != 0){
                System.out.print(
                        h.getFlightByID(s1.getValue()).dep.name +
                        h.getFlightByID(s1.getValue()).arr.name +
                        " "
                );
            }
        }
        System.out.println();
        for (IntVar s1 : S) {
            if(s1.getValue() != 0){
                int date = Math.round(h.getFlightByID(s1.getValue()).date);
                if (date <= 9) System.out.print(date + "  ");
                if (date > 9) System.out.print(date + " ");
            }
        }
        System.out.println();
        for (IntVar c : C) {
            if (c.getValue() != 0) System.out.print(c.getValue() + " ");
        }
        System.out.print("\nAnd z is: ");
        System.out.println(z.getValue());
        System.out.println("And cost_sum is: " + cost_sum.getValue());
    }

    private void printOptSolution(Solution x) {
        System.out.println("z is: " + x.getIntVal(z));
        for (int i = 0; i < x.getIntVal(z); i++) {
//            System.out.print(x.getIntVal(S[i]) + " ");
            System.out.print(h.getFlightByID(x.getIntVal(S[i])).dep.name);
            System.out.print(h.getFlightByID(x.getIntVal(S[i])).arr.name + " ");
        }
        System.out.println();
        for (int i = 0; i < x.getIntVal(z); i++) {
            System.out.print(x.getIntVal(C[i]) + " ");
        }
        System.out.println("\nTotal cost: " + x.getIntVal(cost_sum));
    }

}
