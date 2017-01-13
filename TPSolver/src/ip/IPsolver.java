package ip;

import gurobi.*;
import helpers.HelperMethods;
import main.Airport;
import main.Flight;

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
    private String[] args;
    private int m, n;

    GRBEnv env;
    GRBModel model;
    GRBVar[][] S;

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
        this.h = new HelperMethods(as, fs);
        this.args = args;
    }

    public void getSolution() {
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

            // Set objective

            /*** Add constraints ***/
            GRBLinExpr expr;
            expr = new GRBLinExpr();
            expr.addTerm(1.0, S[n][n]);
            model.addConstr(expr, GRB.EQUAL, 1.0, "End with special flight");

            // I have no idea why I need this. It is taken from sudoku example
            for (int i = 0; i < m; i++) {
                expr = new GRBLinExpr();
                expr.addTerms(null, S[i]);
                String st = "V_" + String.valueOf(i);
                model.addConstr(expr, GRB.EQUAL, 1.0, st);
            }

            this.matrixContraints();
            this.tripProperty1();
            this.tripProperty2();

            // Optimize model
            model.optimize();

            // Print solution
            this.printSolution();

            // Dispose of model and environment
            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
        }
    }

    private void printSolution() throws GRBException {
        double[][] x = model.get(GRB.DoubleAttr.X, S);

        System.out.println();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                if (x[i][j] > 0.5) {
                    System.out.print(1 + " ");
                }
                else {
                    System.out.print(0 + " ");
                }
            }
            System.out.println();
        }

        System.out.println();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                if (x[i][j] > 0.5 && j != m-1) {
                    System.out.print((j+1) + " ");
                }
                if (x[i][j] > 0.5 && j == m-1) {
                    System.out.print(0 + " ");
                }
            }
        }

        System.out.println();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                if (x[i][j] > 0.5 && j != m-1) {
                    System.out.print(
                            h.getFlightByID(j+1).dep.name +
                                    h.getFlightByID(j+1).arr.name +
                                    " "
                    );
                }
            }
        }
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
            for (int j = 0; j < n; j++) {
                expr1.addTerm(1.0, S[i-1][j]);
                expr2.addTerm(1.0, S[i][j]);
            }
            String s1 = "ValidSchedule_" + String.valueOf(i);
            model.addConstr(expr1, GRB.GREATER_EQUAL, expr2, s1);
        }
    }

    private void tripProperty1() throws GRBException {
        GRBLinExpr expr1, expr2;
        Airport a0 = h.getHomePoint();
        ArrayList<Integer> from_home = h.allFrom(a0);
        expr1 = new GRBLinExpr();
        expr2 = new GRBLinExpr();
        for (int j = 0; j < m; j++) {
            if (from_home.contains(j+1)) {
                expr1.addTerm(1.0, S[0][j]);
            }
            else {
                expr2.addTerm(1.0, S[0][j]);
            }
        }
        model.addConstr(expr1, GRB.EQUAL, 1.0, "Trip property 1");
    }

    private void tripProperty2() throws GRBException {
        GRBLinExpr expr1, expr2;
        for (Airport a: this.airports) {
            ArrayList<Integer> all_to = h.allTo(a);
            ArrayList<Integer> all_from = h.allFrom(a);
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
}
