package main;

import org.chocosolver.solver.exception.ContradictionException;
import org.json.simple.*;
import org.json.simple.parser.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Main{

    private static final String FILENAME = "data/test1.json";
    private static final int B = 1000;
    private static ArrayList<Airport> airports = new ArrayList<>();

    public static void main(String[] args) throws ContradictionException, IOException, ParseException {
        readJsonFile(args);
    }

    public static void readJsonFile(String[] args) throws IOException, ParseException {
        JSONParser p = new JSONParser();
        Object obj = p.parse(new FileReader(FILENAME));
        JSONObject jobj = (JSONObject) obj;
        JSONArray jairports = (JSONArray) jobj.get("airports");
        JSONArray jflights = (JSONArray) jobj.get("flights");

        int T = ((Number)jobj.get("holiday_time")).intValue();
        ArrayList<Airport> airports = createAirports(jairports);
        ArrayList<Flight> flights = createFlights(jflights);
        ProblemSolver s = new ProblemSolver(airports, flights, T, B, args);
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

    private static ArrayList<Airport> createAirports(JSONArray jairports) {
        ArrayList<Airport> airports = new ArrayList<>();
        for (int i = 0; i < jairports.size(); i++) {
            JSONObject airport = (JSONObject) jairports.get(i);
            String name = (String) airport.get("name");
            double conn_time = ((Number)airport.get("connection_time")).doubleValue();
            String purpose = (String) airport.get("purpose");
            Airport a = new Airport(name, conn_time, purpose);
            airports.add(a);
        }
        return airports;
    }

    private static Airport getByName(String name){
        for (Airport a : airports) {
            if (a.name.equals(name)) return a;
        }
        return null;
    }
}