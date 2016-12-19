package main;

import org.chocosolver.solver.exception.ContradictionException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Main{

    private static final String FILENAME = "data/test1.in";
    private static final int B = 1000;
    private static ArrayList<Airport> airports = new ArrayList<>();
    private static ArrayList<Flight> flights = new ArrayList<>();
    private static int T;

    private static Airport getByName(String name){
        for (Airport a : airports) {
            if (a.name.equals(name)) return a;
        }
        return null;
    }

    public static void main(String[] args) throws ContradictionException {

        BufferedReader br = null;
        FileReader fr = null;

        try {
            fr = new FileReader(FILENAME);
            br = new BufferedReader(fr);
            String sCurrentLine;
            br = new BufferedReader(new FileReader(FILENAME));
            int parse_helper = 0;

            while ((sCurrentLine = br.readLine()) != null) {
                String firstLetter = String.valueOf(sCurrentLine.charAt(0));
                if (parse_helper == 0 && firstLetter.equals("#")){
                    parse_helper++;
                }
                else if (parse_helper == 1 && firstLetter.equals("#")){
                    parse_helper++;
                }
                else if (parse_helper == 2 && firstLetter.equals("#")){
                    parse_helper++;
                }
                else if (parse_helper == 1) {
                    // read all airports
                    String[] attributes = sCurrentLine.split(", ");
                    Airport a = new Airport(attributes[0],
                                            Float.parseFloat(attributes[1]),
                                            Integer.parseInt(attributes[2])
                    );
                    airports.add(a);
                }
                else if (parse_helper == 2) {
                    // read all flights
                    String[] attributes = sCurrentLine.split(", ");
                    Flight f = new Flight(
                            Integer.parseInt(attributes[0]),
                            getByName(attributes[1]),
                            getByName(attributes[2]),
                            Float.parseFloat(attributes[3]),
                            Float.parseFloat(attributes[4]),
                            Float.parseFloat(attributes[5])
                    );
                    flights.add(f);
                }
                else if (parse_helper == 3){
                    T = Integer.parseInt(sCurrentLine);
                }
            }

        } catch (IOException e) {e.printStackTrace();}
        finally {
            try {
                if (br != null) br.close();
                if (fr != null) fr.close();

            } catch (IOException ex) {ex.printStackTrace();}

        }

        ProblemSolver s = new ProblemSolver(airports, flights, T, B);
        s.getSolution();
    }
}