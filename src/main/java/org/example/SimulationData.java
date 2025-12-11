package org.example;

import java.io.Serializable;
import java.util.ArrayList;

// Klasa transferowa (Data Transfer Object)
public class SimulationData implements Serializable {
    public int m;
    public int[] W;
    public double[] C;
    public int[] L;
    public double[] R;
    public double[] rho;
    public String mpsFilePath;
    public double[][] generatedSSVs;

    public SimulationData(int m, int[] W, double[] C, int[] L, double[] R, double[] rho,
                          String mpsFilePath, double[][] generatedSSVs) {
        this.m = m;
        this.W = W;
        this.C = C;
        this.L = L;
        this.R = R;
        this.rho = rho;
        this.mpsFilePath = mpsFilePath;
        this.generatedSSVs = generatedSSVs;
    }
}