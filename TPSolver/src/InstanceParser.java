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
            printUsage();
            return;
        }
        for (String arg : args) {
            if (arg.equals("-help") || arg.equals("help") || arg.equals("h")) {
                printUsage();
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

        T = ((Number)jobj.get("holiday_time")).intValue();
        createAirports(jairports);
        flights = createFlights(jflights);
        ArrayList<Tuple> tuples = null;
        if (    args.length > 2 &&
                jtuples != null &&
                args[1].equals("-cp") &&
                args[2].equals("-hc2")) {
            tuples = createHC2(jtuples);
        }
        String solFileName = "";
        String solution = "";
        if (args[1].equals("-cp")) {
            modifyData();
            CPsolver s = new CPsolver(airports, flights, T*10, B*100, args, tuples);
            solFileName = getSolFileName("cp");
            solution = s.getSolution();
        }
        else if (args[1].equals("-ip")) {
            IPsolver s = new IPsolver(airports, flights, T, B);
            solFileName = getSolFileName("ip");
            solution = s.getSolution();
        }
        else {
            printUsage();
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
        System.out.println("                       you want to solve,");
        System.out.println("               which must be a .json file containing a list of airports, a list ");
        System.out.println("               of flights and a holiday time. See example instance files for more info.");
        System.out.println("\n    <model> is either:");
        System.out.println("        -cp: the instance will be solved using the TP Constraint Programming Model");
        System.out.println("        -ip: the instance will be solved using the TP Integer Programming Model");
        System.out.println("\n    [options] are applicable only to the CP model and are 0 or more of these parameters: ");
//        System.out.println("        -verbose: prints more information about the solution");
        System.out.println("        -hc2: returns only solutions which comply with hard constraint 2 (HC2), ");
        System.out.println("              specified in the instance input file. HC2 requires that the specified ");
        System.out.println("              destination must be visited at the specified time at least for one day. ");
        System.out.println("        <objective> <objective variable> [-allOpt]: finds optimal solutions, where:");
        System.out.println("            <objective> is either -min or -max");
        System.out.println("            <objective variable> is either -cost, -flights, or -trip_duration, where:");
        System.out.println("                -cost: finds the optional solutions with respect to flights cost");
        System.out.println("                -flights: finds the optional solutions with respect to number of flights");
        System.out.println("                -trip_duration: finds the optional solutions with respect to trip duration");
        System.out.println("        -allOpt: returns all optimal solutions");
        System.out.println("        -all: returns all solutions");
    }
}