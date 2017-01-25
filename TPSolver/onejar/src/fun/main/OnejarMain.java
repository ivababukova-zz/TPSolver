/*
 * Copyright (c) 2004-2010, P. Simon Tuffs (simon@simontuffs.com)
 * All rights reserved.
 *
 * See the full license at http://one-jar.sourceforge.net/one-jar-license.html
 * This license is also included in the distributions of this software
 * under doc/one-jar-license.txt
 */
package fun.main;

import java.util.Arrays;

public class OnejarMain {
    
    public static void main(String args[]) {
        if (args == null)
            args = new String[0];
        System.out.println("onejar main entry point, args=" + Arrays.asList(args));
        new OnejarMain().run();
    }
    
    // Bring up the application: only expected to exit when user interaction
    // indicates so.
    public void run() {
        System.out.println("onejar main is running");
        // Implement the functionality of the application. 
        System.out.println("onejar OK.");
    }
    

}
