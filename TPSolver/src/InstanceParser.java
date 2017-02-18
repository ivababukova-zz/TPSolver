import org.chocosolver.solver.exception.ContradictionException;
import org.json.simple.*;
import org.json.simple.parser.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.util.HashMap;

public class InstanceParser {

    private static int B = 1000000;

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
        String filename = args[0];
        HashMap<String, Airport> airports = null;
        HashMap<Integer, Flight> flights = null;
        HashMap<String, ArrayList<Integer>> depFlights = null;
        HashMap<String, ArrayList<Integer>> arrFlights = null;
        int T = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            String turn = "";
            Boolean consumed = true;
            while ((line = br.readLine()) != null) {
                line = line.replace("[", "").replace("]", "");
                if (line.contains("airports") || line.contains("flights")) {
                    turn = line;
                    consumed = false;
                }
                if (line.contains("holiday")) {
                    String[] props = line.split(", ");
                    T = (int)(Float.parseFloat(props[1])*100);
                    consumed = false;
                }
                else if (!line.contains("airports") && !line.contains("flights") && !line.contains("holiday")) {
                    String[] props = turn.split(", ");
                    String[] lines = line.split(", ");
                    if (props[0].replace("\"", "").trim().equals("flights")) {
                        if (!consumed) {
                            consumed = true;
                            int hashCapacity = Integer.parseInt(props[1]);
                            flights = new HashMap<>(hashCapacity + 1); // +1 for the dummy flight in ip
                        }
                        int id = Integer.parseInt(lines[0]);
                        double date = Float.parseFloat(lines[1]) * 100;
                        double duration = Float.parseFloat(lines[2]) * 100;
                        Airport dep = airports.get(lines[3].replace("\"", "").trim());
                        Airport arr = airports.get(lines[4].replace("\"", "").trim());
                        double price = Float.parseFloat(lines[5]) * 100;
                        Flight f = new Flight(id, dep, arr, date, duration, price);
                        flights.put(id, f);
                        ArrayList<Integer> fromKey = depFlights.get(f.dep.name);
                        ArrayList<Integer> toKey = arrFlights.get(f.arr.name);
                        fromKey.add(id);
                        toKey.add(id);
                        depFlights.put(f.dep.name, fromKey);
                        arrFlights.put(f.arr.name, toKey);
                    }
                    if (props[0].replace("\"", "").trim().equals("airports")) {
                        if (!consumed){
                            consumed = true;
                            int hashCapacity = Integer.parseInt(props[1]);
                            airports = new HashMap<>(hashCapacity);
                            depFlights = new HashMap<>(hashCapacity);
                            arrFlights = new HashMap<>(hashCapacity);
                        }
                        String name = lines[0].replace("\"", "").trim();
                        double conntime = Float.parseFloat(lines[1].replace("\"", "").trim()) * 100;
                        String purpose = lines[2].replace("\"", "").trim();
                        Airport a = new Airport(name, conntime, purpose);
                        airports.put(name, a);
                        depFlights.put(a.name, new ArrayList<>());
                        arrFlights.put(a.name, new ArrayList<>());
                    }
                }
            }
        }
        System.out.println("Reading data in finished.");
        HelperMethods h = new HelperMethods(airports, flights, depFlights, arrFlights);
        if (args[1].equals("-cp")) {
            CPsolver s = new CPsolver(flights, T, B, args, null, null, depFlights, arrFlights, h);
            s.getSolution();
        }
        else if (args[1].equals("-ip")) {
            IPsolver s = new IPsolver(airports, flights, T, B, args, depFlights, arrFlights, h);
            s.getSolution();
        }
        else printUsageEclipse();
    }

    static ArrayList<Tuple> createHC2 (JSONArray jtuples) {
        ArrayList<Tuple> tuples = new ArrayList<>();
//        for (int i = 0; i < jtuples.size(); i++) {
//            JSONObject tuple = (JSONObject) jtuples.get(i);
//            Airport a = getByName((String) tuple.get("airport"));
//            double date = ((Number)tuple.get("date")).doubleValue() * 100;
//            Tuple t = new Tuple(a, date);
//            tuples.add(t);
//        }
        return tuples;
    }

    private static ArrayList<Triplet> createHC1 (JSONArray jtriplets) {
        ArrayList<Triplet> triplets = new ArrayList<>();
//        for (int i = 0; i < jtriplets.size(); i++) {
//            JSONObject triplet = (JSONObject) jtriplets.get(i);
//            Airport a = getByName((String) triplet.get("airport"));
//            double lb = ((Number)triplet.get("lb")).doubleValue() * 100;
//            double ub = ((Number)triplet.get("ub")).doubleValue() * 100;
//            Triplet tr = new Triplet(a, lb, ub);
//            triplets.add(tr);
//        }

        return triplets;
    }

    public static void printUsageEclipse() {
        System.out.println("Usage with Eclipse:"
                + "\n  1. Go to \"Run/Run Configurations ...\""
                + "\n  2. Click on \"Arguments\" and add to \"program arguments\" <filename> <model> [-options],");
        System.out.println("where:\n  <filename> is the relative path to and the name of the TP depFlights you want to solve,");
        System.out.println("             which must be a .json file containing a list of airports, a list of flights and");
        System.out.println("             a holiday time. See example depFlights files for more info.");
        System.out.println("\n    <model> is either:");
        System.out.println("      -cp: the depFlights will be solved using the TP Constraint Programming Model");
        System.out.println("      -ip: the depFlights will be solved using the TP Integer Programming Model");
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
                "\n             The destination and the bounds are specified in the depFlights file.\n");
        System.out.println("      -hc2: finds solutions that comply with hard constraint 2 (HC2). HC2 requires the following:");
        System.out.println("            \"Travellers may require to spend a given date at a given destination\"" +
                "\n            The destination and the date are specified in the depFlights file.\n");
        System.out.println("Example program arguments:"
                + "\n    \"data/small_test.json -cp -all\": this runs the CP solver and prints all solutions for small_test.json depFlights. "
                + "\n    \"data/small_test.json -ip -min -cost -allOpt\": this runs the IP solver and returns all optimal solutions with minimum flights cost");
    }
}