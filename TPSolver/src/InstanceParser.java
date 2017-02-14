import org.chocosolver.solver.exception.ContradictionException;
import org.json.simple.*;
import org.json.simple.parser.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.io.BufferedReader;

public class InstanceParser {

    private static String filename;
    private static int B = 1000000;
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
        readFile(args);
    }
    static void readFile(String[] args) throws IOException, ParseException {
        filename = args[0];
        int T = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            String turn = "";
            while ((line = br.readLine()) != null) {
                line = line.replace("[", "").replace("]", "");
                if (line.equals("airports") || line.equals("flights") || line.equals("holiday")) {
                    turn = line;
                }
                else {
                    String[] lines = line.split(", ");
                    if (turn.equals("flights")) {
                        int id = Integer.parseInt(lines[0]);
                        double date = Float.parseFloat(lines[1]) * 100;
                        double duration = Float.parseFloat(lines[2]) * 100;
                        Airport dep = getByName(lines[3].replace("\"", "").trim());
                        Airport arr = getByName(lines[4].replace("\"", "").trim());
                        double price = Float.parseFloat(lines[5]) * 100;
                        Flight f = new Flight(id, dep, arr, date, duration, price);
                        flights.add(f);
                    }
                    if (turn.equals("airports")) {
                        String name = lines[0].replace("\"", "").trim();
                        double conntime = Float.parseFloat(lines[1]) * 100;
                        String purpose = lines[2].replace("\"", "").trim();
                        Airport a = new Airport(name, conntime, purpose);
                        airports.add(a);
                    }

                    if (turn.equals("holiday")) {
                        T = Integer.parseInt(lines[0])*100;
                    }
                }
            }
        }
        if (args[1].equals("-cp")) {
            CPsolver s = new CPsolver(airports, flights, T, B, args, null, null);
            s.getSolution();
        }
        else if (args[1].equals("-ip")) {
            IPsolver s = new IPsolver(airports, flights, T, B, args);
            s.getSolution();
        }
        else printUsageEclipse();
    }

    static void readJsonFile(String[] args) throws IOException, ParseException {
        filename = args[0];
        JSONParser p = new JSONParser();
        Object obj = p.parse(new FileReader(filename));
        JSONObject jobj = (JSONObject) obj;
        JSONArray jairports = (JSONArray) jobj.get("airports");
        JSONArray jflights = (JSONArray) jobj.get("flights");
        JSONArray jtuples = (JSONArray) jobj.get("hard_constraint_2");
        JSONArray jtriplets = (JSONArray) jobj.get("hard_constraint_1");

        int T = ((Number)jobj.get("holiday_time")).intValue() * 100;
        createAirports(jairports);
        flights = createFlights(jflights);
        ArrayList<Tuple> tuples = null;
        ArrayList<Triplet> triplets = null;
        if (args.length > 2 && args[1].equals("-cp")) {
            if (jtuples != null && args[2].equals("-hc2")) tuples = createHC2(jtuples);
            if (jtriplets != null && args[2].equals("-hc1")) triplets = createHC1(jtriplets);
        }
        if (args[1].equals("-cp")) {
            CPsolver s = new CPsolver(airports, flights, T, B, args, tuples, triplets);
            s.getSolution();
        }
        else if (args[1].equals("-ip")) {
            IPsolver s = new IPsolver(airports, flights, T, B, args);
            s.getSolution();
        }
        else printUsageEclipse();
    }

    static ArrayList<Flight> createFlights(JSONArray jflights){
        ArrayList<Flight> flights = new ArrayList<>();
        for(int i = 0; i < jflights.size(); i++) {
            JSONObject flight = (JSONObject) jflights.get(i);
            int id = ((Number)flight.get("id")).intValue();
            Airport dep = getByName((String) flight.get("dep_airport"));
            Airport arr = getByName((String) flight.get("arr_airport"));
            double date = ((Number)flight.get("date")).doubleValue() * 100;
            double duration = ((Number)flight.get("duration")).doubleValue() * 100;
            double price = ((Number)flight.get("price")).doubleValue() * 100;
            Flight f = new Flight(id, dep, arr, date, duration, price);
            flights.add(f);
        }
        return flights;
    }

    static void createAirports(JSONArray jairports) {
        for (int i = 0; i < jairports.size(); i++) {
            JSONObject airport = (JSONObject) jairports.get(i);
            String name = (String) airport.get("name");
            double conn_time = ((Number)airport.get("connection_time")).doubleValue() * 100;
            String purpose = (String) airport.get("purpose");
            Airport a = new Airport(name, conn_time, purpose);
            airports.add(a);
        }
    }

    static ArrayList<Tuple> createHC2 (JSONArray jtuples) {
        ArrayList<Tuple> tuples = new ArrayList<>();
        for (int i = 0; i < jtuples.size(); i++) {
            JSONObject tuple = (JSONObject) jtuples.get(i);
            Airport a = getByName((String) tuple.get("airport"));
            double date = ((Number)tuple.get("date")).doubleValue() * 100;
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
            double lb = ((Number)triplet.get("lb")).doubleValue() * 100;
            double ub = ((Number)triplet.get("ub")).doubleValue() * 100;
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

    public static void printUsageEclipse() {
        System.out.println("Usage with Eclipse:"
                + "\n  1. Go to \"Run/Run Configurations ...\""
                + "\n  2. Click on \"Arguments\" and add to \"program arguments\" <filename> <model> [-options],");
        System.out.println("where:\n  <filename> is the relative path to and the name of the TP instance you want to solve,");
        System.out.println("             which must be a .json file containing a list of airports, a list of flights and");
        System.out.println("             a holiday time. See example instance files for more info.");
        System.out.println("\n    <model> is either:");
        System.out.println("      -cp: the instance will be solved using the TP Constraint Programming Model");
        System.out.println("      -ip: the instance will be solved using the TP Integer Programming Model");
        System.out.println("\n    [options] are optional. They can be 0 or more of the following parameters:\n");
        System.out.println("        <objective> <objective variable> [-allOpt]: finds optimal solutions, where:");
        System.out.println("          -allOpt: is an optional flag. It returns all optimal solutions.");
        System.out.println("          <objective> is either -min or -max.\n");
        System.out.println("          <objective variable> is either -cost, -flights, -trip_duration or -connections, where:");
        System.out.println("              -cost: finds the optional solutions with respect to flights cost.");
        System.out.println("              -flights: finds the optional solutions with respect to number of flights.");
        System.out.println("              -trip_duration: finds the optional solutions with respect to trip duration.");
        System.out.println("              -connections: finds the optional solutions with respect to number of flights" +
                           "\n                            to connection airports. It is implemented only for CP.\n");
        System.out.println("The following parameters are applicable only for the CP model:");
        System.out.println("      -all: returns all solutions.\n");
        System.out.println("      -hc1: finds solutions that comply with hard constraint 1 (HC1). HC1 requires the following:");
        System.out.println("            \"Travellers may wish to spend a certain amount of consecutive days at a given destination," +
                "\n             specified by both upper and lower bounds.\" " +
                "\n             The destination and the bounds are specified in the instance file.\n");
        System.out.println("      -hc2: finds solutions that comply with hard constraint 2 (HC2). HC2 requires the following:");
        System.out.println("            \"Travellers may require to spend a given date at a given destination\"" +
                "\n            The destination and the date are specified in the instance file.\n");
        System.out.println("Example program arguments:"
                + "\n    \"data/small_test.json -cp -all\": this runs the CP solver and prints all solutions for small_test.json instance. "
                + "\n    \"data/small_test.json -ip -min -cost -allOpt\": this runs the IP solver and returns all optimal solutions with minimum flights cost");
    }
}