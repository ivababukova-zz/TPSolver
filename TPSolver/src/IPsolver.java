import gurobi.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * Created by ivababukova on 1/11/17.
 */
public class IPsolver {

    private ArrayList<Flight> flights;
    private ArrayList<Airport> airports;
    private HelperMethods h;
    private int T;
    private int B; // upper bound on the cost
    private int m, n;
    private String solution;
    private String[] args;

    GRBEnv env;
    GRBModel model;
    GRBVar[][] S;
    GRBVar[][] N;

    public IPsolver(
            ArrayList<Airport> as,
            ArrayList<Flight> fs,
            int T,
            int B,
            String[] args
            ) {
        this.flights = fs;
        this.airports = as;
        this.T = T;
        this.B = B;
        this.h = new HelperMethods(as, fs, T);
        this.solution = "";
        this.addSpecialFlight();
        this.args = args;
    }

    public String getSolution() throws IOException {
        try {
            env = new GRBEnv("tp.log");
            model = new GRBModel(env);
            this.n = flights.size() - 1;
            this.m = n + 1; // add the extra flight
            // Create Xi,j
            S = new GRBVar[m][m];

            for (int i = 0; i < m; i++) {
                for (int j = 0; j < m; j++) {
                    String st = "X_" + String.valueOf(i) + "_" + String.valueOf(j);
                    S[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                }
            }

            /*** Add constraints ***/
            GRBLinExpr expr;
            expr = new GRBLinExpr();
            expr.addTerm(1.0, S[n][n]);
            model.addConstr(expr, GRB.EQUAL, 1.0, "End with special flight");

            this.matrixContraints();
            this.tripProperty1();
            this.tripProperty2();
            this.tripProperties3and4();
            this.tripProperty5();
            this.optimiseWhat();

            getAllSols();
            model.getEnv().set("OutputFlag", "0"); // set to 1 to get gurobi custom output
            // Optimize model
            model.optimize();
            this.debugModel();

            // Print and return solution
            printAllSolutions();

            // Dispose of model and environment
            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
        }
        return this.solution;
    }

    private void matrixContraints() throws GRBException {
        GRBLinExpr expr;

        // there must be 1 in only one row in the matrix
        for (int i = 0; i < m; i++) {
            expr = new GRBLinExpr();
            for (int j = 0; j < m; j++) {
                expr.addTerm(1.0, S[i][j]);
            }
            String s1 = "R_" + String.valueOf(i);
            model.addConstr(expr, GRB.EQUAL, 1.0, s1);
        }

        // there must be 1 in at most one column that does not represent special flight
        for (int j = 0; j < n; j++) {
            expr = new GRBLinExpr();
            for (int i = 0; i < n; i++) {
                expr.addTerm(1.0, S[i][j]);
            }
            String s1 = "C_" + String.valueOf(j);
            model.addConstr(expr, GRB.LESS_EQUAL, 1.0, s1);
        }

        this.specialFlightConstr();
    }

    private void specialFlightConstr() throws GRBException{
        GRBLinExpr expr, expr1, expr2;

        // there can be more than one special flight scheduled
        expr = new GRBLinExpr();
        for (int i = 0; i < m; i++) {
            expr.addTerm(1.0, S[i][n]);
        }
        model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "One or more special flights");

        // once a special flight is scheduled, no other flights can be scheduled
        for (int i = 1; i < m; i++) {
            expr1 = new GRBLinExpr();
            expr2 = new GRBLinExpr();
            expr1.addTerm(1.0, S[i-1][n]);
            expr2.addTerm(1.0, S[i][n]);
            String s1 = "ValidSchedule_" + String.valueOf(i);
            model.addConstr(expr1, GRB.LESS_EQUAL, expr2, s1);
        }
    }

    private void tripProperty1() throws GRBException {
        GRBLinExpr expr1;
        Airport a0 = h.getHomePoint();
        ArrayList<Integer> from_home = h.allFromAirport(a0);
        expr1 = new GRBLinExpr();
        for (int j: from_home) {
            expr1.addTerm(1.0, S[0][j - 1]);
        }
        model.addConstr(expr1, GRB.EQUAL, 1.0, "Trip property 1");
    }

    private void tripProperty2() throws GRBException {
        GRBLinExpr expr1, expr2;
        for (Airport a: this.airports) {
            ArrayList<Integer> all_to = h.allToAirport(a);
            ArrayList<Integer> all_from = h.allFromAirport(a);
            for (int i = 1; i < m; i++) {
                expr1 = new GRBLinExpr();
                expr2 = new GRBLinExpr();
                for (int j1 : all_to) {
                    expr1.addTerm(1.0, S[i-1][j1-1]);
                }
                for (int j2 : all_from) {
                    expr2.addTerm(1.0, S[i][j2-1]);
                }
                model.addConstr(expr1, GRB.EQUAL, expr2, "Trip property 2 " + a.name);
            }
        }
    }

    private void tripProperties3and4() throws GRBException {
        GRBLinExpr expr;
        for (int i = 0; i < n - 1; i++) {
            for (int next = 0; next < m; next++) {
                expr = new GRBLinExpr();
                ArrayList<Integer> disallowed_prev = h.disallowedPrev(next+1);
                for (int prev : disallowed_prev) {
                    expr.addTerm(1.0, S[i][prev-1]);
                }
                expr.addTerm(1.0, S[i+1][next]);
                model.addConstr(expr, GRB.LESS_EQUAL, 1, "Trip Properties 3 and 4");
            }
        }
    }

    private void tripProperty5() throws GRBException {
        GRBLinExpr expr;
        ArrayList<Airport> D =  h.getDestinations();
        for (Airport d : D) {
            ArrayList<Integer> all_to = h.allToAirport(d);
            expr = new GRBLinExpr();
            for(int i = 0; i < n; i++) {
                for (int j : all_to) {
                    expr.addTerm(1.0, S[i][j-1]);
                }
            }
            model.addConstr(expr, GRB.GREATER_EQUAL, 1, "Trip Property 5");
        }
    }

    /*** Objective functions for optimisation ***/

    // minimise or maximise the cost of the trip
    private void costObj(Boolean toMinimise) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        for(int i = 0; i < m; i++) {
            for(int j = 0; j < m; j++) {
                expr.addTerm(h.getFlightByID(j+1).cost, S[i][j]);
            }
        }
        if (toMinimise) model.setObjective(expr, GRB.MINIMIZE);
        else model.setObjective(expr, GRB.MAXIMIZE);
    }

    // minimise or maximise the number of taken flights
    private void noFlightsObj(Boolean toMinimise) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        for (int i = 0; i < m; i++) {
            expr.addTerm(1.0, S[i][n]);
        }
        if (toMinimise) model.setObjective(expr, GRB.MAXIMIZE); // more dummy flights means less real flights
        else model.setObjective(expr, GRB.MINIMIZE); // more dummy flights means less real flights
    }

    // minimise or maximise the duration of the trip
    private void tripDurationObj(Boolean toMinimise) throws GRBException {
        Airport a0 = h.getHomePoint();
        ArrayList<Integer> toHome = h.allToAirport(a0);
        int dummy = toHome.size() - 1;
        toHome.remove(dummy);
        GRBVar[][] Y = createNarray(n - 1, toHome.size());
        GRBLinExpr potentialLast;
        GRBLinExpr trip_duration = new GRBLinExpr();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < toHome.size(); j++) {
                int fj = toHome.get(j) - 1;
                // magic, equivalent to Y[i][nj] = S[i][fj] * S[i+1][n]
                    // If Y[i][j] = 0, then S[i][fj] is not the last flight.
                    // If Y[i][j] = 1, then S[i][fj] is the last flight and we add the sum of its date and duration
                model.addConstr(Y[i][j], GRB.GREATER_EQUAL, 0, "");
                model.addConstr(Y[i][j], GRB.LESS_EQUAL, S[i][fj], "");
                model.addConstr(Y[i][j], GRB.LESS_EQUAL, S[i+1][n], "");
                potentialLast = new GRBLinExpr();
                potentialLast.addTerm(1.0, S[i][fj]);
                potentialLast.addTerm(1.0, S[i+1][n]);
                potentialLast.addConstant(-1);
                // end of magic

                model.addConstr(Y[i][j], GRB.GREATER_EQUAL, potentialLast, "");
                // the value of last is equal to the sum of the date and duration of the last flight
                trip_duration.addTerm(h.getFlightByID(fj+1).date + h.getFlightByID(fj+1).duration, Y[i][j]);
            }
        }
        if (toMinimise) model.setObjective(trip_duration, GRB.MINIMIZE);
        else model.setObjective(trip_duration, GRB.MAXIMIZE);

    }

    // helper function for trip duration optimisation
    private GRBVar[][] createNarray(int size1, int size2) throws GRBException {
        N = new GRBVar[size1][size2];
        System.out.println("Number of to home flights: " + size2);
        for (int i = 0; i < size1; i++) {
            for (int j = 0; j < size2; j++) {
                String st = "N_" + String.valueOf(i) + "_" + String.valueOf(j);
                N[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
            }
        }
        return N;
    }

     /*** end of objective functions code ***/


    private void getAllSols() throws GRBException {
        // Limit the search space by setting a gap for the worst possible solution that will be accepted
        model.set(GRB.DoubleParam.PoolGap, 1);
        // do a systematic search for the k-best solutions
        model.set(GRB.IntParam.PoolSearchMode, 2);

        int nSolutions = model.get(GRB.IntAttr.SolCount);
        System.out.println("Number of solutions found: " + nSolutions);
    }

    private void printAllSolutions() throws GRBException, IOException {
        System.out.println();
        if (allRequired()) {
            for (int k = 0; k < model.get(GRB.IntAttr.SolCount); ++k) {
                model.set(GRB.IntParam.SolutionNumber, k);
                double[][] x = model.get(GRB.DoubleAttr.Xn, S);
                printSolution(x);
            }
        } else {
            double[][] x = model.get(GRB.DoubleAttr.X, S);
            printSolution(x);
        }
    }

    private void printSolution(double[][] x) throws GRBException {
        double cost = 0;

        System.out.println();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                if (x[i][j] > 0.5 && j != m-1) {
                    String flightMssg = (j+1) + " ";
                    System.out.print(flightMssg);
                    this.solution += flightMssg;
                }
            }
        }

        System.out.println();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                if (x[i][j] > 0.5 && j != m-1) {
                    cost = cost + h.getFlightByID(j+1).cost;
                    System.out.println(
                            MessageFormat.format(
                            "Flight with id {0} from {1} to {2} on date: {3} costs: {4}",
                                    (j+1),
                                    h.getFlightByID(j + 1).dep.name,
                                    h.getFlightByID(j + 1).arr.name,
                                    h.getFlightByID(j + 1).date / 100,
                                    h.getFlightByID(j + 1).cost / 100
                            )
                    );
                }
            }
        }
        String costMssg = "Total cost: " + cost / 100;
        System.out.println(costMssg);
        this.solution += costMssg + "\n";
    }

    private void debugModel() throws GRBException {
        int status = model.get(GRB.IntAttr.Status);
        if (status == GRB.Status.UNBOUNDED) {
            System.out.println("The model cannot be solved "
                    + "because it is unbounded");
            return;
        }
        if (status == GRB.Status.OPTIMAL) {
            System.out.println("The optimal objective is " +
                    model.get(GRB.DoubleAttr.ObjVal));
            return;
        }
        if (status != GRB.Status.INF_OR_UNBD &&
                status != GRB.Status.INFEASIBLE    ){
            System.out.println("Optimization was stopped with status " + status);
            return;
        }

        // Compute IIS
        System.out.println("The model is infeasible; computing IIS");
        model.computeIIS();
        System.out.println("\nThe following constraint(s) "
                + "cannot be satisfied:");
        for (GRBConstr c : model.getConstrs()) {
            if (c.get(GRB.IntAttr.IISConstr) == 1) {
                System.out.println(c.get(GRB.StringAttr.ConstrName));
            }
        }
    }

    private void addSpecialFlight(){
        Airport a0 = h.getHomePoint();
        Flight special = new Flight(flights.size()+1, a0, a0, T, 0, 0);
        flights.add(special);
    }

    private Boolean allRequired() {
        for (String arg: args) {
            if (arg.equals("-allOpt")) return true;
        }
        return false;
    }

    private void optimiseWhat() throws GRBException {
        Boolean isMin = true;
        for (String str : args) {
            if (str.equals("-min")) {
                System.out.print("Optimal solutions with minimum ");
                isMin = true;
                break;
            }
            else if (str.equals("-max")) {
                System.out.print("Optimal solutions with maximum ");
                isMin = false;
                break;
            }
        }
        for (String str : args) {
            if (str.equals("-cost")) {
                System.out.println("cost:\n");
                costObj(isMin);
            }
            if (str.equals("-flights")) {
                System.out.println("number of flights:\n");
                noFlightsObj(isMin);
            }
            if (str.equals("-trip_duration")) {
                System.out.println("trip duration:\n");
                tripDurationObj(isMin);
            }
            if (str.equals("-hc1") || str.equals("-hc2") || str.equals("-all") || str.equals("-connections")) {
                System.out.println("\nThere is no support for this option: " + str + ". I will return a solution with minimum cost instead.");
                costObj(true);
            }
        }
    }

    private void multiObj() throws GRBException {
        model.set(GRB.IntAttr.NumObj, 2);
        int SetObjPriority[] = new int[] {1, 1};
        double SetObjWeight[] = new double[] {0.5, 0.5};
        model.set(GRB.IntAttr.ObjNPriority, SetObjPriority[0]);
        model.set(GRB.DoubleAttr.ObjNWeight, SetObjWeight[0]);
        model.set(GRB.IntParam.ObjNumber, 0);
//        model.set(GRB.DoubleAttr.ObjN, "", "", 0, 0);
    }
}
