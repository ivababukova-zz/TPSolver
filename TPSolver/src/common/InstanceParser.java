package common;

import ip.IPsolver;
import cp.CPsolver;
import org.chocosolver.solver.exception.ContradictionException;
import org.json.simple.*;
import org.json.simple.parser.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class InstanceParser {

    private static final String FILENAME = "data/small_test.json";
    private static int B = 1000000;
    private static int T = 0;
    private static ArrayList<Airport> airports = new ArrayList<>();
    private static ArrayList<Flight> flights = new ArrayList<>();

    public static void main(String[] args) throws ContradictionException, IOException, ParseException {
        readJsonFile(args);
    }

    public static void readJsonFile(String[] args) throws IOException, ParseException {
        JSONParser p = new JSONParser();
        Object obj = p.parse(new FileReader(FILENAME));
        JSONObject jobj = (JSONObject) obj;
        JSONArray jairports = (JSONArray) jobj.get("airports");
        JSONArray jflights = (JSONArray) jobj.get("flights");
        JSONArray jtuples = (JSONArray) jobj.get("hard_constraint_2");

        T = ((Number)jobj.get("holiday_time")).intValue();
        createAirports(jairports);
        flights = createFlights(jflights);
        ArrayList<Tuple> tuples = new ArrayList<>();
        if (jtuples != null) tuples = createHC2(jtuples);

        CPsolver s = new CPsolver(airports, flights, T*10, B*100, args, tuples);
//        IPsolver s = new IPsolver(airports, flights, T, B, args);
        if (s.getClass().equals(CPsolver.class)) {
            modifyData();
        }
        s.getSolution();
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
            double date = ((Number)tuple.get("date")).doubleValue();
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
}