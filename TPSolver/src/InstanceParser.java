import gnu.trove.impl.sync.TSynchronizedRandomAccessIntList;
import org.chocosolver.solver.exception.ContradictionException;
import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class InstanceParser {

    private static String filename;
    private static int B = 1000000;
    private static int T = 0;
    private static ArrayList<Airport> airports = new ArrayList<>();
    private static ArrayList<Flight> flights = new ArrayList<>();

    public static void main(String[] args) throws ContradictionException, IOException, ParseException {
        if (args.length < 2) {
            printUsageEclipse();
            return;
        }
        for (String arg : args) {
            if (arg.equals("-help") || arg.equals("help") || arg.equals("h")) {
                printUsageEclipse();
                return;
            }
        }
        readJsonFile(args);
    }

    private static void readJsonFile(String[] args) throws IOException, ParseException {
        filename = args[0];

        JSONParser p = new JSONParser();
        Object obj = p.parse(new FileReader(filename));
        JSONObject jobj = (JSONObject) obj;
        JSONArray jairports = (JSONArray) jobj.get("airports");
        JSONArray jflights = (JSONArray) jobj.get("flights");
        JSONArray jtuples = (JSONArray) jobj.get("hard_constraint_2");
        JSONArray jtriplets = (JSONArray) jobj.get("hard_constraint_1");

        T = ((Number)jobj.get("holiday_time")).intValue();
        createAirports(jairports);
        flights = createFlights(jflights);
        ArrayList<Tuple> tuples = null;
        ArrayList<Triplet> triplets = null;
        if (args.length > 2 && args[1].equals("-cp")) {
            if (jtuples != null && args[2].equals("-hc2")) tuples = createHC2(jtuples);
            if (jtriplets != null && args[2].equals("-hc1")) triplets = createHC1(jtriplets);
        }
        String solFileName = "";
        String solution = "";
        if (args[1].equals("-cp")) {
            modifyData();
            CPsolver s = new CPsolver(airports, flights, T*10, B*100, args, tuples, triplets);
            solFileName = getSolFileName("cp");
            solution = s.getSolution();
        }
        else if (args[1].equals("-ip")) {
            IPsolver s = new IPsolver(airports, flights, T, B, args);
            solFileName = getSolFileName("ip");
            solution = s.getSolution();
        }
        else {
            printUsageEclipse();
            return;
        }
//        writeSolutionToFile(solFileName, solution);
    }

    private static String getSolFileName(String model) {
        String[] filenameParts = filename.split("/");
        String parentFolders = "";
        for (int i = 0; i < filenameParts.length - 1; i++) {
            parentFolders += filenameParts[i] + "/";
        }
        String name = filenameParts[filenameParts.length - 1];
        filenameParts = name.split("\\.");

        String finalName = parentFolders + "solutions/" + model + "/" + filenameParts[0] + ".sol";
//        System.out.println("Solution will be saved in file " + finalName + "\n");
        return finalName;
    }

    private static void writeSolutionToFile(String solFileName, String solutions) throws IOException {
        FileWriter fw = new FileWriter(solFileName);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(solutions);
        bw.close();
        fw.close();
    }

    private static ArrayList<Flight> createFlights(JSONArray jflights){
        ArrayList<Flight> flights = new ArrayList<>();
        for(int i = 0; i < jflights.size(); i++) {
            JSONObject flight = (JSONObject) jflights.get(i);
            int id = ((Number)flight.get("id")).intValue();
            Airport dep = getByName((String) flight.get("dep_airport"));
            Airport arr = getByName((String) flight.get("arr_airport"));
            double date = ((Number)flight.get("date")).doubleValue();
            double duration = ((Number)flight.get("duration")).doubleValue();
            double price = ((Number)flight.get("price")).doubleValue();
            Flight f = new Flight(id, dep, arr, date, duration, price);
            flights.add(f);
        }
        return flights;
    }

    private static void createAirports(JSONArray jairports) {
        for (int i = 0; i < jairports.size(); i++) {
            JSONObject airport = (JSONObject) jairports.get(i);
            String name = (String) airport.get("name");
            double conn_time = ((Number)airport.get("connection_time")).doubleValue();
            String purpose = (String) airport.get("purpose");
            Airport a = new Airport(name, conn_time, purpose);
            airports.add(a);
        }
    }

    private static ArrayList<Tuple> createHC2 (JSONArray jtuples) {
        ArrayList<Tuple> tuples = new ArrayList<>();
        for (int i = 0; i < jtuples.size(); i++) {
            JSONObject tuple = (JSONObject) jtuples.get(i);
            Airport a = getByName((String) tuple.get("airport"));
            double date = ((Number)tuple.get("date")).doubleValue() * 10;
            Tuple t = new Tuple(a, date);
            tuples.add(t);
        }
        return tuples;
    }

    private static ArrayList<Triplet> createHC1 (JSONArray jtriplets) {
        ArrayList<Triplet> triplets = new ArrayList<>();
        for (int i = 0; i < jtriplets.size(); i++) {
            JSONObject triplet = (JSONObject) jtriplets.get(i);
            Airport a = getByName((String) triplet.get("airport"));
            double lb = ((Number)triplet.get("lb")).doubleValue() * 10;
            double ub = ((Number)triplet.get("ub")).doubleValue() * 10;
            Triplet tr = new Triplet(a, lb, ub);
            triplets.add(tr);
        }

        return triplets;
    }

    private static Airport getByName(String name){
        for (Airport a : airports) {
            if (a.name.equals(name)) return a;
        }
        return null;
    }

    private static void modifyData(){
        for(Flight f: flights){
            f.cost = f.cost*100;
            f.duration = f.duration*10;
            f.date = f.date*10;
        }
        for(Airport a: airports) {
            a.conn_time = a.conn_time*10;
        }
    }

    private static void printUsage() {
        System.out.println("Usage:\n    java -jar TPSolver1.jar <filename> <model> [-options]");
        System.out.println("Where:\n    <filename> is the relative path to and the name of the TP instance");
        System.out.println(        "               you want to solve, which must be a .json file, containing");
        System.out.println(        "               a list of airports, a list of flights and a holiday time.");
        System.out.println(        "               See example instance files for more info.");
        System.out.println("\n    <model> is either:");
        System.out.println("            -cp: the instance will be solved using the TP Constraint Programming Model");
        System.out.println("            -ip: the instance will be solved using the TP Integer Programming Model");
        System.out.println("\n    [options] are applicable only to the CP model and are 0 or more of these parameters:\n");
        System.out.println("        -allOpt: returns all optimal solutions.\n");
        System.out.println("        -all: returns all solutions.\n");
        System.out.println("        -hc1: finds solutions that comply with hard constraint 1 (HC1). HC1 requires the following:");
        System.out.println("              \"Travellers may wish to spend a certain amount of consecutive days at a given destination," +
                "\n               specified by both upper and lower bounds.\" " +
                "\n              The destination and the bounds are specified in the instance file.\n");
        System.out.println("        -hc2: finds solutions that comply with hard constraint 2 (HC2). HC2 requires the following:");
        System.out.println("              \"Travellers may require to spend a given date at a given destination\"" +
                "\n              The destination and the date are specified in the instance file.\n");
        System.out.println("        <objective> <objective variable> [-allOpt]: in case you want to find optimal solutions, where:\n");
        System.out.println("            <objective> is either -min or -max.\n");
        System.out.println("            <objective variable> is either -cost, -flights, or -trip_duration, where:\n");
        System.out.println("                -cost: finds the optional solutions with respect to flights cost.\n");
        System.out.println("                -flights: finds the optional solutions with respect to number of flights.\n");
        System.out.println("                -trip_duration: finds the optional solutions with respect to trip duration.\n");
        System.out.println("                -connections: finds the optional solutions with respect to number of flights" +
                "\n                              to connection airports.\n");
    }

    private static void printUsageJar() {
        System.out.println("Usage:\n    java -jar TPSolver1.jar <filename> -cp [-options]");
        System.out.println("Where:\n    <filename> is the relative path to and the name of the TP instance");
        System.out.println(        "               you want to solve, which must be a .json file, containing");
        System.out.println(        "               a list of airports, a list of flights and a holiday time.");
        System.out.println(        "               See example instance files for more info.");
        System.out.println("\n    [options] are 0 or more of the following parameters:\n");
        System.out.println("        -allOpt: returns all optimal solutions.\n");
        System.out.println("        -all: returns all solutions.\n");
        System.out.println("        -hc1: finds solutions that comply with hard constraint 1 (HC1). HC1 requires the following:");
        System.out.println("              \"Travellers may wish to spend a certain amount of consecutive days at a given destination," +
                "\n               specified by both upper and lower bounds.\" " +
                "\n              The destination and the bounds are specified in the instance file.\n");
        System.out.println("        -hc2: finds solutions that comply with hard constraint 2 (HC2). HC2 requires the following:");
        System.out.println("              \"Travellers may require to spend a given date at a given destination\"" +
                "\n              The destination and the date are specified in the instance file.\n");
        System.out.println("        <objective> <objective variable> [-allOpt]: in case you want to find optimal solutions, where:\n");
        System.out.println("            <objective> is either -min or -max.\n");
        System.out.println("            <objective variable> is either -cost, -flights, or -trip_duration, where:\n");
        System.out.println("                -cost: finds the optional solutions with respect to flights cost.\n");
        System.out.println("                -flights: finds the optional solutions with respect to number of flights.\n");
        System.out.println("                -trip_duration: finds the optional solutions with respect to trip duration.\n");
        System.out.println("                -connections: finds the optional solutions with respect to number of flights" +
                "\n                              to connection airports.\n");
        System.out.println("\nExample: \"java -jar data/small_test.json -cp -min -trip_duration -allOpt\" \n         returns all solutions with minimum duration of the trip.");
    }


    private static void printUsageEclipse() {
        System.out.println("Usage with Eclipse:"
                + "\n    1. Go to \"Run/Run Configurations ...\""
                + "\n    2. Click on \"Arguments\" and add to \"program arguments\" <filename> <model> [-options],");
        System.out.println("where:\n    <filename> is the relative path to and the name of the TP instance you want to solve,");
        System.out.println("               which must be a .json file containing a list of airports, a list of flights and");
        System.out.println("               a holiday time. See example instance files for more info.");
        System.out.println("\n    <model> is either:");
        System.out.println("        -cp: the instance will be solved using the TP Constraint Programming Model");
        System.out.println("        -ip: the instance will be solved using the TP Integer Programming Model");
        System.out.println("\n    [options] are applicable only to the CP model and are optional. They can be 0 or more of the following parameters:\n");
        System.out.println("        -allOpt: returns all optimal solutions.\n");
        System.out.println("        -all: returns all solutions.\n");
        System.out.println("        -hc1: finds solutions that comply with hard constraint 1 (HC1). HC1 requires the following:");
        System.out.println("              \"Travellers may wish to spend a certain amount of consecutive days at a given destination," +
                           "\n               specified by both upper and lower bounds.\" " +
                           "\n              The destination and the bounds are specified in the instance file.\n");
        System.out.println("        -hc2: finds solutions that comply with hard constraint 2 (HC2). HC2 requires the following:");
        System.out.println("              \"Travellers may require to spend a given date at a given destination\"" +
                           "\n              The destination and the date are specified in the instance file.\n");
        System.out.println("        <objective> <objective variable> [-allOpt]: in case you want to find optimal solutions, where:\n");
        System.out.println("            <objective> is either -min or -max.\n");
        System.out.println("            <objective variable> is either -cost, -flights, or -trip_duration, where:\n");
        System.out.println("                -cost: finds the optional solutions with respect to flights cost.\n");
        System.out.println("                -flights: finds the optional solutions with respect to number of flights.\n");
        System.out.println("                -trip_duration: finds the optional solutions with respect to trip duration.\n");
        System.out.println("                -connections: finds the optional solutions with respect to number of flights" +
                           "\n                              to connection airports.\n");
        System.out.println("Example program arguments:"
                + "\n    \"data/small_test.json -cp -all\": this runs the CP solver and prints all solutions for small_test.json instance. "
                + "\n    \"data/small_test.json -cp -min -cost -allOpt\": this runs the CP solver and returns all optimal solutions with minimum flights cost");
    }
}