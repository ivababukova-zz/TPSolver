import java.util.ArrayList;
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

    ArrayList<Flight> flights; // all flights
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
    IntVar[] C; // C[i] is equal to the cost of flight S[i]
    IntVar cost_sum; // the total cost of the flights schedule

    IntVar trip_duration;

    IntVar[] isConnection;
    IntVar connections_count; // the number of connection flights taken during the trip

    public CPsolver(
            ArrayList<Airport> as,
            ArrayList<Flight> fs,
            int holiday_time,
            int ub,
            String[] arguments,
            ArrayList<Tuple> tups,
            ArrayList<Triplet> tris
    ){
        flights = fs;
        T = holiday_time;
        B = ub;
        h = new HelperMethods(as, fs, T);
        args = arguments;
        tuples = tups;
        triplets = tris;
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
        int[] to_home = h.arrayToint(h.allToAirport(a0)); // all flights arriving from a0
        int[] from_home = h.arrayToint(h.allFromAirport(a0)); // all flights departing from a0

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
            int[] all_to = h.arrayToint(h.allToAirport(d)); // all flights that fly to d
            if (all_to.length == 0) {
                System.out.println("It is impossible to visit destination " + d.name + ".\nThe instance has no solution.");
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
        int[] all_to = h.arrayToint(h.allToAirport(a));
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


    public String getSolution() {
        if (init() == 0) {
            return "";
        }

        if (args.length == 2 || (args.length == 3 && args[2].equals("-hc2"))) {
            Solution x = solver.findSolution();
            if (x == null) {
                System.out.println("No solution was found");
            }
            else {
                System.out.println("A solution:");
                printSolution(x);
            }
            return getStats();
        }
        Boolean m = null;
        IntVar[] to_optimise = new IntVar[4];
        int objSize = 0;
        int isOptimalSearch = 0;

        for (String arg : args) {
            if (arg.equals("-min")) {
                System.out.print("Solution with minimum ");
                m = Model.MINIMIZE;
                isOptimalSearch += 1;
            }
            if (arg.equals("-max")) {
                System.out.print("Solution with maximum ");
                m = Model.MAXIMIZE;
                isOptimalSearch += 1;
            }
            if (arg.equals("-cost")) {
                System.out.println("cost:");
                to_optimise[objSize] = this.cost_sum;
                objSize ++;
                isOptimalSearch += 1;
            }
            if (arg.equals("-flights")) {
                System.out.println("number of flights:");
                to_optimise[objSize] = this.z;
                objSize ++;
                isOptimalSearch += 1;
            }
            if (arg.equals("-trip_duration")) {
                System.out.println("trip duration:");
                to_optimise[objSize] = this.trip_duration;
                isOptimalSearch += 1;
            }
            if (arg.equals("-connections")) {
                System.out.println("number of flights to connection airports:");
                to_optimise[objSize] = this.connections_count;
                objSize ++;
                isOptimalSearch += 1;
            }
            if (arg.equals("-allOpt")) {
                System.out.println("All optimal solutions are:");
                printAllSols(solver.findAllOptimalSolutions(to_optimise[0], m));
                return getStats();
            }
            if (arg.equals("-all")) {
                System.out.println("All solutions are:");
                printAllSols(solver.findAllSolutions());
                return getStats();
            }
        }
        if (isOptimalSearch == 2 && objSize == 1) {
            returnOneOptimal(m, to_optimise[0]);
        } else if (isOptimalSearch == 1) {
            System.out.println("\nNot enough arguments provided\n");
            InstanceParser.printUsageEclipse();
            return "";
        } else if (objSize > 1) {
            multiobjective(new IntVar[] {cost_sum, trip_duration}, m);
        }
        return getStats();
    }

    private void multiobjective(IntVar[] objectives, Boolean goal) {
        System.out.println("Doing multiobjective optimisation:");
        ParetoOptimizer po = new ParetoOptimizer(goal, objectives);
        solver.plugMonitor(po);
        while (solver.solve()) {
            List<Solution> paretoFront = po.getParetoFront();
            printAllSols(paretoFront);
        }
    }

    private String getStats() {
        String stats = "nodes: " + solver.getMeasures().getNodeCount() +
                "   cpu: " + solver.getMeasures().getTimeCount();
        System.out.println(stats);
        return stats;
    }

    private void returnOneOptimal(Boolean m, IntVar to_optimise) {
        Solution x = solver.findOptimalSolution(to_optimise, m);
        if (x == null) {
            System.out.println("No optimal solution was found");
            return;
        }
        printSolution(x);
    }

    private void printAllSols(List<Solution> solutions) {
        for (Solution sol : solutions) {
            if (sol != null) printSolution(sol);
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
