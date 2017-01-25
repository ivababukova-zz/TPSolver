import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Created by ivababukova on 12/19/16.
 */
public class UserInterface extends JFrame{
    JTextField file_name = new JTextField();
    JButton submit = new JButton();
    JLabel jLabel1 = new JLabel();
    Container container;

    String tp_instace;
    boolean error = false;

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

    public UserInterface() {
        initComponents();
    }

    private void initComponents() {
        JFrame f = new JFrame("TP Solver");
        f.setSize(600, 500);
        container = f.getContentPane();
        container.setLayout(new BorderLayout());

        // Add a window listener for close button
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        jLabel1.setText("Problem instance: ");
        jLabel1.setSize(60, 10);
        submit.setText("Submit");
        submit.setSize(40, 40);

        // This is an empty content area in the frame
        container.add(jLabel1, BorderLayout.NORTH);
        container.add(submit, BorderLayout.SOUTH);
        container.add(file_name, BorderLayout.CENTER);
        submit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }


    /**
     *   This is the method that is called when an action is performed.
     *   Over here I just simply show an error message if any of the text fields are empty or just show their names.
     */
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        this.tp_instace = file_name.getText();
        if ("".equals(tp_instace)) {error = true;}
        if (error) System.exit(0);
//        if (!error) initialising(tp_instace);
    }

//    public static void initialising(String FILENAME) {
//        BufferedReader br = null;
//        FileReader fr = null;
//
//        try {
//            fr = new FileReader(FILENAME);
//            br = new BufferedReader(fr);
//            String sCurrentLine;
//            br = new BufferedReader(new FileReader(FILENAME));
//            int parse_helper = 0;
//
//            while ((sCurrentLine = br.readLine()) != null) {
//                String firstLetter = String.valueOf(sCurrentLine.charAt(0));
//                if (parse_helper == 0 && firstLetter.equals("#")){
//                    parse_helper++;
//                }
//                else if (parse_helper == 1 && firstLetter.equals("#")){
//                    parse_helper++;
//                }
//                else if (parse_helper == 2 && firstLetter.equals("#")){
//                    parse_helper++;
//                }
//                else if (parse_helper == 1) {
//                    // read all airports
//                    String[] attributes = sCurrentLine.split(", ");
//                    Airport a = new Airport(attributes[0],
//                            Float.parseFloat(attributes[1]),
//                            Integer.parseInt(attributes[2])
//                    );
//                    airports.add(a);
//                }
//                else if (parse_helper == 2) {
//                    // read all flights
//                    String[] attributes = sCurrentLine.split(", ");
//                    Flight f = new Flight(
//                            Integer.parseInt(attributes[0]),
//                            getByName(attributes[1]),
//                            getByName(attributes[2]),
//                            Float.parseFloat(attributes[3]),
//                            Float.parseFloat(attributes[4]),
//                            Float.parseFloat(attributes[5])
//                    );
//                    flights.add(f);
//                }
//                else if (parse_helper == 3){
//                    T = Integer.parseInt(sCurrentLine);
//                }
//            }
//
//        } catch (IOException e) {e.printStackTrace();}
//        finally {
//            try {
//                if (br != null) br.close();
//                if (fr != null) fr.close();
//
//            } catch (IOException ex) {ex.printStackTrace();}
//
//        }
//
//        CPsolver s = new CPsolver(airports, flights, T, B);
//        s.getSolution();
//    }
}

