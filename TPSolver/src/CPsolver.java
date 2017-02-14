import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.*;
import org.chocosolver.solver.objective.ParetoOptimizer;
import org.chocosolver.solver.variables.*;

/**
 * Created by ivababukova on 12/16/16.
 * this is the CP solver for TP
 */
public class CPsolver {

    HashMap<Integer, Flight> flights; // all flights
    HashMap<String, ArrayList<Integer>> depFlights;
    HashMap<String, ArrayList<Integer>> arrFlights;
    HelperMethods h;
    int T; // holiday time
    int B; // upper bound on the total flights cost
    String[] args;
    ArrayList<Tuple> tuples; // an array of (airport a, date d) that means that traveller must be at a at time d
    ArrayList<Triplet> triplets; // for hard constraint 1

    /** The main model without optimisations and hard and soft constraints: */
    Model model;
    Solver solver;
    IntVar[] S; // the flights schedule
    IntVar z; // the number of flights in the schedule

    /** Additional variables */
    IntVar[] C, isConnection; // C[i] is equal to the cost of flight S[i]
    IntVar cost_sum, trip_duration, connections_count;

    public CPsolver(
            HashMap<String, Airport> as,
            HashMap<Integer, Flight> fs,
            int holiday_time,
            int ub,
            String[] arguments,
            ArrayList<Tuple> tups,
            ArrayList<Triplet> tris,
            HashMap<String, ArrayList<Integer>> dep,
            HashMap<String, ArrayList<Integer>> arr
    ){
        flights = fs;
        T = holiday_time;
        B = ub;
        h = new HelperMethods(as, fs, T, dep);
        args = arguments;
        tuples = tups;
        triplets = tris;
        depFlights = dep;
        arrFlights = arr;
    }

    private int init(){
        model = new Model("TP CPsolver");
        S = model.intVarArray("Flights Schedule", flights.size() + 1, 0, flights.size());
        z = model.intVar("End of schedule", 2, flights.size());
        solver = model.getSolver();
        C = model.intVarArray("The cost of each taken flight", flights.size() + 1, 0, 5000000);
        cost_sum = model.intVar(0, B); // the total cost of the trip
        trip_duration = model.intVar(1, T);
        connections_count = model.intVar(0, flights.size());
        isConnection = model.boolVarArray(flights.size() + 1); // isConnection[i] = 1 if flight i+1 is connecting

        if (findSchedule() == 0) return 0; // trivial failure
        return 1;
    }

    private int findSchedule() {
        Airport a0 = h.getHomePoint(); // the home point
        int[] to_home = h.arrayToint(arrFlights.get(a0.name)); // all flights arriving from a0
        int[] from_home = h.arrayToint(depFlights.get(a0.name)); // all flights departing from a0

        model.member(S[0], from_home).post(); // trip property 1
        model.arithm(S[1], "!=", 0).post(); // S can not be empty
        model.arithm(connections_count, "<=", z).post();
        model.sum(C, "=", cost_sum).post(); // the trip cost = the sum of the cost of all taken flights:
        model.sum(isConnection, "=", connections_count).post(); // the number of connection flights

        if (tripProperty5() == 0) return 0; // trivial failure
        if (triplets != null) hardConstraint1(); // if hc1 is required, impose it
        if (tuples != null) hardConstraint2(); // if hc2 is required, impose it

        for(int i = 1; i <= flights.size(); i++) {
            Flight f = h.getFlightByID(i);
            tripProperties2and3and4(f); // impose trip properties 2, 3 and 4
            sequenceConstraints(i, to_home, f); // impose valid sequence rules
            costAndConnectionsCountConstraints(i);
            lastFlightConstraint(i);
        }
        this.model.allDifferentExcept0(S).post(); // the same flight can be taken only once
        return 1;
    }

    private void lastFlightConstraint(int i) {
        for (int j = 1; j <= flights.size(); j++) {
            Flight f = h.getFlightByID(j);
            model.ifThen(
                    model.and(
                            model.arithm(z, "=", i),
                            model.arithm(S[i-1], "=", j)
                    ),
                    model.arithm(trip_duration, "=", (int) (f.date + f.duration))
            );
        }
    }

    // set the values of C
    private void costAndConnectionsCountConstraints(int i){
        for (int j = 0; j <= flights.size(); j++) {
            this.model.ifThen(
                    model.arithm(S[j], "=", i),
                    model.arithm(C[j], "=", (int) h.getFlightByID(i).cost)
            );
            model.ifThenElse(
                        model.member(S[j], h.arrayToint(h.allConnectionFlights())),
                        model.arithm(isConnection[j], "=", 1),
                        model.arithm(isConnection[j], "=", 0)
            );
        }
    }

    // enforces trip properties 2, 3 and 4
    private void tripProperties2and3and4(Flight f){
        int[] allowed_next = h.arrayToint(h.allowedNextFlights(f)); // trip property 3
        int[] allowed_last = h.arrayToint(h.allowedLastFlights(f)); // trip property 4

        // todo there is no need to enforce trip property 4, it is already enforced
        // when last flight is constrained to arrive at the home point
        for (int j = 1; j < flights.size(); j++) {
            model.ifThen(
                    model.and(
                            model.arithm(S[j-1],"=", f.id),
                            model.arithm(z, ">", j+1)),
                    model.member(S[j], allowed_next)
            );
            model.ifThen(
                    model.and(
                            model.arithm(S[j-1],"=", f.id),
                            model.arithm(z, "=", j+1)),
                    model.member(S[j], allowed_last)
            );
        }
    }

    // all destinations must be visited
    private int tripProperty5(){
        for (Airport d: h.getDestinations()) {
            int[] all_to = h.arrayToint(arrFlights.get(d.name)); // all flights that fly to d
            if (all_to.length == 0) {
                System.out.println("It is impossible to visit destination " + d.name + ".\nThe depFlights has no solution.");
                return 0;
            }
            IntVar X = this.model.intVar(1, all_to.length);
            this.model.among(X, S, all_to).post(); // S must contain at least one flight that goes to d
        }
        return 1;
    }

    private void sequenceConstraints(int i, int[] to_home, Flight f){
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

        // if s[i] is not 0, then s[i-1] must not be 0
        model.ifThen(
                model.arithm(S[i], "!=", 0),
                model.arithm(S[i-1], "!=", 0)
        );

        this.model.ifThen(
                model.arithm(S[i], "=", 0),
                model.arithm(C[i], "=", 0)
        );

        this.model.ifThen(
                model.arithm(S[i], "!=", 0),
                model.arithm(C[i], "!=", 0)
        );
    }

    /*** HARD CONSTRAINT 1 CODE ***/
    private void hardConstraint1(){
        System.out.println("Searching for solutions with HC1:");
        IntVar[] D = model.intVarArray(
                "Destinations with hard constr 1",
                triplets.size() + 1,
                0,
                flights.size());
        model.arithm(D[0], "=", 0).post(); // D[0] is not important
        model.allDifferent(D).post();
        int index = 1;
        for (Triplet tri : this.triplets) {
            Airport a = tri.getA();
            a.setIndex(index);
            double lb = tri.getLb();
            double ub = tri.getUb();
            this.hc1(D, a, lb, ub, index);
            index ++;
        }
    }

    private void hc1(IntVar[] D, Airport a, double lb, double ub, int index) {
        int[] all_to = h.arrayToint(arrFlights.get(a.name));
        for (int i = 1; i <= flights.size(); i++) {
            model.ifThen(
                    model.arithm(D[index], "=", i),
                    model.member(S[i-1], all_to)
            );
            for (int prev : all_to) {
                int[] next = h.allowedNextFlightsHC1(prev, lb, ub);
                model.ifThen(
                        model.arithm(S[i-1], "=", prev),
                        model.member(S[i], next)
                );
            }
        }
    }

    /*** end of hard constraint 1 code ***/

    /*** HARD CONSTRAINT 2 CODE ***/
    private void hardConstraint2(){
        System.out.println("Searching for solutions with HC2 for following dates and destinations:");
        IntVar[] D = model.intVarArray(
                "Destinations with hard constr 2",
                tuples.size() + 1,
                0,
                flights.size());
        model.arithm(D[0], "=", 0).post(); // D[0] is not important
        model.allDifferent(D).post();
        int index = 1;
        for (Tuple tup : this.tuples) {
            Airport a = tup.getA();
            a.setIndex(index);
            double date = tup.getDate();
            this.dateLocationConstraint(D, a, date, index);
            index ++;
        }
    }

    // hard constraint 2
    private void dateLocationConstraint(IntVar[] D, Airport a, double date, int index) {
        System.out.println("Be at destination " + a.name + " at date " + date/100);
        int[] all_to_before = h.arrayToint(h.allToBefore(a, date)); // all flights to desired destination
        int[] all_from_after = h.arrayToint(h.allFromAfter(a, date)); // all flights from desired destination

        for(int j = 1; j <= flights.size(); j++) {
            model.ifThen(
                    model.arithm(D[index], "=", j),
                    model.and(
                            model.member(S[j-1], all_to_before),
                            model.member(S[j], all_from_after)
                    )
            );
        }
    }

    /*** end of hard constraint 2 code ***/


    public void getSolution() {
        if (init() == 0) return;

        String response = "";
        Boolean m = true, allRequired = false;
        ArrayList<IntVar> to_optimise = new ArrayList<>();

        for (String arg : args) {
            if (arg.equals("-min")) {response += "minimum "; m = Model.MINIMIZE;}
            if (arg.equals("-max")) {response += "maximum "; m = Model.MAXIMIZE;}
            if (arg.equals("-cost")) {response += "cost, "; to_optimise.add(this.cost_sum);}
            if (arg.equals("-flights")) {response += "number of flights, "; to_optimise.add(this.z);}
            if (arg.equals("-trip_duration")) {response += "trip duration,"; to_optimise.add(this.trip_duration);}
            if (arg.equals("-connections")) {response += "connections, "; to_optimise.add(this.connections_count);}
            if (arg.equals("-allOpt") || arg.equals("-all")) allRequired = true;
        }

        if (to_optimise.size() <= 1) {
            if (!allRequired) getSingleSolution(m, to_optimise, response);
            else getAllSolutions(m, to_optimise, response);
        }

        if (to_optimise.size() > 1) multiobjective(to_optimise, m, response);

        System.out.println("nodes: " + solver.getMeasures().getNodeCount() +
                "   cpu: " + solver.getMeasures().getTimeCount());
    }

    private void getSingleSolution(Boolean m, ArrayList<IntVar> to_optimise, String response) {
        System.out.println("Single solution with " + response + ":");
        Solution x;
        if (to_optimise.size() > 0) x = solver.findOptimalSolution(to_optimise.get(0), m);
        else x = solver.findSolution();
        if (x == null) {
            System.out.println("No solution was found");
            return;
        }
        printSolution(x);
    }

    private void getAllSolutions(Boolean m, ArrayList<IntVar> to_optimise, String response) {
        System.out.println("Multiple solutions with " + response + ":");
        List<Solution> solutions;
        if (to_optimise.size() > 0) solutions = solver.findAllOptimalSolutions(to_optimise.get(0), m);
        else solutions = solver.findAllSolutions();
        for (Solution sol : solutions) {
            if (sol != null) printSolution(sol);
        }
    }

    private void multiobjective(ArrayList<IntVar> obj, Boolean goal, String response) {
        IntVar[] objectives = new IntVar[obj.size()];
        obj.toArray(objectives);
        System.out.println("Doing multiobjective optimisation with " + response + ":");
        ParetoOptimizer po = new ParetoOptimizer(goal, objectives);
        solver.plugMonitor(po);
        while (solver.solve()) {
            List<Solution> paretoFront = po.getParetoFront();
            for (Solution sol : paretoFront) {
                if (sol != null) printSolution(sol);
            }
        }
    }

    private void printSolution(Solution x) {
        System.out.print("  ");
        for (int i = 0; i < x.getIntVal(z); i++) {
            System.out.print(x.getIntVal(S[i]) + " ");
        }
        System.out.println();
        for (int i = 0; i < x.getIntVal(z); i++) {
            String nextVar = x.getIntVal(S[i]) + " ";
            System.out.print("  Flight with id " + nextVar);
            System.out.print("from " + h.getFlightByID(x.getIntVal(S[i])).dep.name);
            System.out.print(" to " + h.getFlightByID(x.getIntVal(S[i])).arr.name);
            System.out.print(" on date: " + h.getFlightByID(x.getIntVal(S[i])).date / 100);
            System.out.println(" costs: " + h.getFlightByID(x.getIntVal(S[i])).cost / 100);
        }
        System.out.print("  Trip duration: " + (x.getIntVal(trip_duration) / 100.0));
        System.out.print(" Total cost: " + (x.getIntVal(cost_sum) / 100.0));
        System.out.println(" Number of connections: " + (x.getIntVal(connections_count)) + "\n");
    }
}