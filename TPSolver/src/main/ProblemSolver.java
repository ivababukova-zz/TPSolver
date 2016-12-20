package main;

import java.util.ArrayList;

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

    private Model model;
    private Solver solver;
    private IntVar[] S;
    private IntVar z;
    private IntVar[] C;
    private IntVar cost_sum;

    public ProblemSolver(ArrayList<Airport> as, ArrayList<Flight> fs, int T, int B){
        this.airports = as;
        this.flights = fs;
        this.T = T;
        this.B = B;
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
        this.model = new Model("TP ProblemSolver");
        this.S = model.intVarArray("Flights Schedule", flights.size() + 1, 0, flights.size());
        this.z = model.intVar("End of schedule", 2, flights.size());

        Flight cheapest = h.getCheapestAfter(0);
        Flight secondCheapest = h.getCheapestAfter(cheapest.cost);
        int costLB = Math.round(cheapest.cost + secondCheapest.cost);

        this.C = this.model.intVarArray("The cost of each taken flight", flights.size() + 1, 0, 5555); // todo instead of B as upper bound, make upper bound = most expensive flight
        this.cost_sum = this.model.intVar(0, 5000);

        this.model.sum(C, "=", cost_sum).post();
        this.solver = model.getSolver();


        Airport a0 = h.getHomePoint();
        int[] to_home = h.arrayToint(h.allToHome(a0, this.T));
        int[] from_home = h.arrayToint(h.allFrom(a0));

        for (int i: to_home) {
            System.out.println(i);
        }

        model.member(S[0], from_home).post();
        this.model.arithm(S[1], "!=", 0).post();

        destinationConstraint();
        costConstraint();

        for(int i = 1; i <= flights.size(); i++) {
            Flight f = h.getFlightByID(i);

            timeConstraint(f);

            this.model.ifThen(
                    model.arithm(S[i], "=", 0),
                    model.arithm(C[i], "=", 0)
            );

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
//                    model.arithm(S[i-1], "=", 18)
                    model.member(S[i-1], to_home)
            );

        }
        this.model.allDifferentExcept0(S).post();

        // todo when the instance has no solutions, we need to get an error
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

    private void min_cost(){
        this.model.setObjective(Model.MINIMIZE, this.cost_sum);
    }

    private void min_flights(){
//        this.model.setObjective(Model.MINIMIZE, this.z);

    }

    public void getSolution(){
        init();
//        min_cost();
        solver.solve();
//        solver.findOptimalSolution(this.z, Model.MINIMIZE);
        printSolution();
//        while (solver.solve()) {
//            printSolution();
//        }
    }

    private void printSolution(){
        for (IntVar s1 : S) {
            if(s1.getValue() != 0){
                System.out.print(h.getFlightByID(
                        s1.getValue()).dep.name +
                        "" +
                        h.getFlightByID(s1.getValue()).arr.name +
                        ", "
                );
            }
            else{
                System.out.print(s1.getValue() + ", ");
            }
        }
        System.out.println();
        for (IntVar c : C) {
                System.out.print(c.getValue() + ", ");
        }
        System.out.print("\nAnd z is: ");
        System.out.println(z.getValue());
        System.out.println("And cost_sum is: " + cost_sum.getValue());

    }

}
