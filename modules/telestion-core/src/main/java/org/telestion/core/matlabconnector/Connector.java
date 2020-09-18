/*
Author Liam Franssen
Copyright Liam Franssen
 */

package org.telestion.core.matlabconnector;

import  com.mathworks.engine.*;
import java.util.Scanner;


public class Connector {

    Scanner sg=new Scanner(System.in);
    //public static void main(String[] args) {
    //}
    public Connector(){

    }

    public static void main(String[] args) throws Exception {
        Scanner s=new Scanner(System.in);
        //Future<MatlabEngine> engFuture = MatlabEngine.startMatlabAsync();
        //MatlabEngine eng = engFuture.get();
        MatlabEngine eng = MatlabEngine.startMatlab();
        //eng.eval("RunRungeKuttaSimulationWind");

        System.out.println("want to close Matlab");
        String k=s.next();
        if (k.equals("no") ||k.equals("n")) {
            eng.close();
        }else {

        }
    }


    public void rungwind(MatlabEngine engine) throws Exception{
        engine.eval("RunRungeKuttaSimulationWind");

    }

    public void runAnyMatlabFunc(MatlabEngine engine,String fun) throws Exception{

        engine.feval(fun);

    }
    public void runAnyMatlabScript(MatlabEngine engine,String script) throws Exception{
        engine.eval(script);

    }
    public void runAnyMatlabFc(MatlabEngine engine,String fun) throws Exception{
        engine.eval("RunRungeKuttaSimulationWind");

    }
}
