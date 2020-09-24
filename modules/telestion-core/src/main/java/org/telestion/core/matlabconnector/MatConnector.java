/*
Author Liam Franssen
Copyright Liam Franssen
 */

package org.telestion.core.matlabconnector;

import com.mathworks.engine.MatlabEngine;

import java.util.Scanner;


public class MatConnector {

    Scanner sg = new Scanner(System.in);
    private MatlabEngine engine;

    //public static void main(String[] args) {
    //}
    public MatConnector() throws Exception {
        MatlabEngine enginek = MatlabEngine.startMatlab();
        this.engine = enginek;
    }

    public MatConnector(MatlabEngine eng) throws Exception {
        this.engine = eng;
    }

    public static void main(String[] args) throws Exception {
        Scanner s = new Scanner(System.in);
        //Future<MatlabEngine> engFuture = MatlabEngine.startMatlabAsync();
        //MatlabEngine eng = engFuture.get();
        //MatlabEngine eng = MatlabEngine.startMatlab();
        //eng.eval("RunRungeKuttaSimulationWind");
        MatConnector mc = new MatConnector();
        mc.rungwind(mc.engine);
        System.out.println("want to close Matlab");
        String k = s.next();
        if (k.equals("no") || k.equals("n")) {
            
        } else {
            mc.close();
        }
    }

    public void close() throws Exception {
        this.engine.close();
    }

    public void rungwind(MatlabEngine engine) throws Exception {
        engine.eval("RunRungeKuttaSimulationWind");

    }

    public void runAnyMatlabFuncinAsk(MatlabEngine engine) throws Exception {
        System.out.println("1. Input is How many Outputs has the function \n" +
                "2. The functions name \n" +
                "3. The functions input parameter can be multiple inputs");
        int hmo = sg.nextInt();
        String name = sg.next();
        System.out.println("Inputs?");
        String k = sg.next();
        //System.out.println("How many inputs?");
        //int inp=sg.nextInt();
        //Objects[] opts = new Objects[inp];
        Object[] opt;
        int p = 0;
        if (k.equals("yes") || k.equals("y")) {
            System.out.println("How many inputs?");
            int inp = sg.nextInt();
            Object[] opts = new Object[inp];
            p = inp;
            for (int i = 0; i < inp; i++) {

                System.out.println("what kind of input?(all small, type of varible)");
                String in = sg.next();
                if (in.equals("double")) {
                    opts[i] = sg.nextDouble();

                } else if (in.equals("int")) {
                    opts[i] = sg.nextInt();

                } else if (in.equals("float")) {
                    opts[i] = sg.nextFloat();

                }

            }
            opt = opts;
            if (p == 1) {
                Object[] results = engine.feval(hmo, name, opt[0]);
            } else if (p == 2) {
                Object[] results = engine.feval(hmo, name, opt[0], opt[1]);
            } else if (p == 3) {
                Object[] results = engine.feval(hmo, name, opt[0], opt[1], opt[2]);
            } else if (p == 4) {
                Object[] results = engine.feval(hmo, name, opt[0], opt[1], opt[2], opt[3]);
            }
        }
        //if (p==0) {
        Object[] results = engine.feval(hmo, name);
        /*}else if (p==1) {
            Object[] results = engine.feval(hmo, name,opt[0]);
        }else if (p==2) {
            Object[] results = engine.feval(hmo, name,opt[0],opt[1]);
        }else if (p==3) {
            Object[] results = engine.feval(hmo, name,opt[0],opt[1],opt[2]);
        }else if (p==4) {
            Object[] results = engine.feval(hmo, name,opt[0],opt[1],opt[2],opt[3]);
        }*/
    }

    public void runAnyMatlabFuncin(MatlabEngine engine, int hmo, int hmi, String name, Object[] opt) throws Exception {
        /*
        System.out.println("1. Input is How many Outputs has the function \n" +"2. The functions name \n" +"3. The functions input parameter can be multiple inputs");
        int hmo = sg.nextInt();
        String name = sg.next();
        System.out.println("Inputs?");
        String k = sg.next();
        System.out.println("How many inputs?");
        int inp=sg.nextInt();
        Objects[] opts = new Objects[inp];
        Object[] opt;
        */
        int p = 0;
        //if (k.equals("yes") || k.equals("y")) {
        //System.out.println("How many inputs?");
        int inp = hmi;
        Object[] opts = new Object[hmi];
        p = inp;
            /*for (int i = 0; i < inp; i++) {

                //System.out.println("what kind of input?(all small, type of varible)");
                //String in = sg.next();
                if (opt[i])) {
                    opts[i] = sg.nextDouble();

                } else if (in.equals("int")) {
                    opts[i] = sg.nextInt();

                } else if (in.equals("float")) {
                    opts[i] = sg.nextFloat();

                }

            }*/
        opt = opts;
        if (p == 1) {
            Object[] results = engine.feval(hmo, name, opt[0]);
        } else if (p == 2) {
            Object[] results = engine.feval(hmo, name, opt[0], opt[1]);
        } else if (p == 3) {
            Object[] results = engine.feval(hmo, name, opt[0], opt[1], opt[2]);
        } else if (p == 4) {
            Object[] results = engine.feval(hmo, name, opt[0], opt[1], opt[2], opt[3]);
        }
        //}
        //if (p==0) {
        Object[] results = engine.feval(hmo, name);
        /*}else if (p==1) {
            Object[] results = engine.feval(hmo, name,opt[0]);
        }else if (p==2) {
            Object[] results = engine.feval(hmo, name,opt[0],opt[1]);
        }else if (p==3) {
            Object[] results = engine.feval(hmo, name,opt[0],opt[1],opt[2]);
        }else if (p==4) {
            Object[] results = engine.feval(hmo, name,opt[0],opt[1],opt[2],opt[3]);
        }*/
    }


    public void runAnyMatlabScript(MatlabEngine engine, String script) throws Exception {
        engine.eval(script);

    }

    public void runAnyMatlabFuncin(MatlabEngine engine, String fun, Object[] opts) throws Exception {


    }

    public void runAnyMatlabFuncinString(MatlabEngine engine, String fun, String[] opts) throws Exception {
        engine.feval(fun, opts);

    }

    public void runAnyMatlabFuncinDouble(MatlabEngine engine, String fun, double[] opts) throws Exception {
        engine.feval(fun);

    }
}
