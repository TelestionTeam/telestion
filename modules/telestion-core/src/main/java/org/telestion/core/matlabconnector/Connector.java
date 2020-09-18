/*
Author Liam Franssen
Copyright Liam Franssen
 */

package org.telestion.core.matlabconnector;

import  com.mathworks.engine.*;
import java.util.Scanner;


public class Connector {


    //public static void main(String[] args) {
    //}
    public static void main(String[] args) throws Exception {
        Scanner s=new Scanner(System.in);
        //Future<MatlabEngine> engFuture = MatlabEngine.startMatlabAsync();
        //MatlabEngine eng = engFuture.get();
        MatlabEngine eng = MatlabEngine.startMatlab();
        eng.eval("RunRungeKuttaSimulationWind");
        System.out.println("want to close");
        String k=s.next();
        if (k.equals("no") ||k.equals("n")) {

        }else {
            eng.close();
        }
    }

    public void rungwind(){

    }
}
