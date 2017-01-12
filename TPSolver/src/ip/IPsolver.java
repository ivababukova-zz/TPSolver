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
    private HelperMethods h;
    private int T;
    private int B; // upper bound on the cost
    private String[] args;

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
        this.T = T;
        this.B = B;
        this.h = new HelperMethods(as, fs);
        this.args = args;
    }

    public void getSolution() {
        try {
            env = new GRBEnv("tp.log");
            model = new GRBModel(env);
            int n = 5;
            int m = n + 1; // add the extra flight
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
            GRBLinExpr expr, expr1, expr2;
            expr = new GRBLinExpr();
            expr.addTerm(1.0, S[n][n]);
            model.addConstr(expr, GRB.EQUAL, 1.0, "special flight");

            for (int i = 0; i < m; i++) {
                expr = new GRBLinExpr();
                expr.addTerms(null, S[i]);
                String st = "V_" + String.valueOf(i);
                model.addConstr(expr, GRB.EQUAL, 1.0, st);
            }

            // there should be only one 1 in each roll and column
            // i.e. at most one flight is taken at each step i
            // and no flight is taken more than once
            for (int i = 0; i < m; i++) {
                expr = new GRBLinExpr();
                for (int j = 0; j < m; j++) {
                    expr.addTerm(1.0, S[i][j]);
                }
                String s1 = "R_" + String.valueOf(i);
                model.addConstr(expr, GRB.EQUAL, 1.0, s1);
            }

            for (int j = 0; j < n; j++) {
                expr = new GRBLinExpr();
                for (int i = 0; i < n; i++) {
                    expr.addTerm(1.0, S[i][j]);
                }
                String s1 = "C_" + String.valueOf(j);
                model.addConstr(expr, GRB.LESS_EQUAL, 1.0, s1);
            }

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

            // Optimize model
            model.optimize();

            // Print solution
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

            // Dispose of model and environment
            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
        }
    }
}
