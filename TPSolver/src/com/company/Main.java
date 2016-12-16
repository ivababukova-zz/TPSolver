package com.company;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;


public class Main{

    private static final String FILENAME = "data/test_instance1.in";


    public static void main(String[] args) {
        BufferedReader br = null;
        FileReader fr = null;
        Model model = new Model("my first problem");

        try {

            fr = new FileReader(FILENAME);
            br = new BufferedReader(fr);

            String sCurrentLine;

            br = new BufferedReader(new FileReader(FILENAME));

            while ((sCurrentLine = br.readLine()) != null) {
                System.out.println(sCurrentLine);
            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (br != null)
                    br.close();

                if (fr != null)
                    fr.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }

    }
}