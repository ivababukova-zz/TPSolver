package main;

import java.util.ArrayList;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

/**
 * Created by ivababukova on 12/16/16.
 * this is the CP solver for TP
 */
public class Solver {

    ArrayList<Airport> airports;
    ArrayList<Flight> flights;
    Model model;
    IntVar[] S;
    int T;

    public Solver(ArrayList<Airport> as, ArrayList<Flight> fs, int T){
        this.airports = as;
        this.flights = fs;
        this.T = T;
    }

    private void init(){
        this.model = new Model("TP Solver");
        this.S = model.intVarArray("Flights Schedule", flights.size(), 0, flights.size());
        IntVar z = model.intVar("End of schedule", 1, flights.size()-1);

        for(int i = 0; i < flights.size()-1; i++) {
            model.ifThen(
                    model.arithm(z, "=", i),
                    model.arithm(S[i], "=", 0)
            );
            model.ifThen(
                    model.arithm(S[i], "=", 0),
                    model.arithm(S[i+1], "=", 0)
            );
        }
        this.model.allDifferentExcept0(S).post();

        // todo the dep and arr airports of two consecutive flights must be the same

        // todo the first flight must be from the home point

        // todo the last flight must be to the home point

        // todo the duration + time + conn time constraint

        // todo the total time of the schedule must be less or equal to T

    }

    public void solve(){
        init();
        model.getSolver().solve();
        for (IntVar s: S ){
            System.out.print(s.getValue() + " ");
        }
        System.out.println("\nThat's it!");
    }
}
