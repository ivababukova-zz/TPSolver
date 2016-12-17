package main;

import java.util.ArrayList;

import helpers.HelperMethods;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.*;
import org.chocosolver.solver.constraints.nary.count.*;

/**
 * Created by ivababukova on 12/16/16.
 * this is the CP solver for TP
 */
public class Solver {

    private ArrayList<Airport> airports;
    private ArrayList<Flight> flights;
    private HelperMethods h;
    private Model model;
    private IntVar[] S;
    private IntVar z;
    private int T;

    public Solver(ArrayList<Airport> as, ArrayList<Flight> fs, int T){
        this.airports = as;
        this.flights = fs;
        this.T = T;
        this.h = new HelperMethods(as, fs);
    }

    private void init(){
        this.model = new Model("TP Solver");
        this.S = model.intVarArray("Flights Schedule", flights.size() + 1, 0, flights.size());
        this.z = model.intVar("End of schedule", 2, flights.size());

        Airport a0 = h.getHomePoint();
        int[] to_home = h.arrayToint(h.allTo(a0));
        int[] from_home = h.arrayToint(h.allFrom(a0));

        model.member(S[0], from_home).post();

        for(int i = 1; i <= flights.size(); i++) {
            Flight f = h.getFlightByID(i);
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

            // the last non-zero flight should arrive at the home point
            for (int zet = 2; zet <= flights.size(); zet++) {
                model.ifThen(
                        model.arithm(z, "=", zet),
                        model.member(S[zet-1], to_home)
                );
            }
        }
        this.model.allDifferentExcept0(S).post();
        
        // all destinations must be visited
        for (Airport d: h.getDestinations()) {
            int[] all_to = h.arrayToint(h.allTo(d));
            IntVar X = this.model.intVar(1, all_to.length);
            this.model.among(X, S, all_to).post();
        }

        // todo the duration + time + conn time constraint

        // todo the total time of the schedule must be less or equal to T

        // todo when the instance has no solutions, we need to get an error

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

    public void solve(){
        init();
        //model.getSolver().propagate();
//        model.getSolver().solve();
        while (model.getSolver().solve()) {
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
        }
//        for (IntVar s : S) {
//            if(s.getValue() != 0){
//                System.out.print(h.getFlightByID(
//                        s.getValue()).dep.name +
//                        "" +
//                        h.getFlightByID(s.getValue()).arr.name +
//                        ", "
//                );
//            }
//            else{
//                System.out.print(s.getValue() + ", ");
//            }
//        }
        System.out.print("\nAnd z is: ");
        System.out.println(z.getValue());
    }
}
