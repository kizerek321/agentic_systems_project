package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class MFN {
    public int m; // number of links
    public int[] W; // component number vector
    public double[] C; // component capacity vector
    public int[] L; //  lead time vector
    public double[] R; // component reliability vector
    public double[] rho; //  vector of the correlation between the faults of the components
    public double[] beta; // beta vector
    public ArrayList<int[]> MPs; // list of minimal paths

    public MFN(int m, int[] W, double[] C, int[] L, double[] R, double[] rho) {
        if (W.length != m || C.length != m || L.length != m || R.length != m || rho.length != m) {
            throw new IllegalArgumentException("Incorrect lenght of vectors, need to be: " + m);
        }

        for (int i = 0; i < m; i++) {
            if (R[i] < 0 || R[i] > 1 || rho[i] < 0 || rho[i] > 1) {
                throw new IllegalArgumentException("values of R and rho must be in range [0, 1]");
            }
        }

        this.m = m;
        this.W = W;
        this.C = C;
        this.L = L;
        this.R = R;
        this.rho = rho;
        this.MPs = new ArrayList<>();

        this.beta = new double[m];
        for (int i = 0; i < m; i++) {
            // beta_i = 1 + (rho_i * (1 - r_i)) / r_i
            this.beta[i] = 1.0 + (this.rho[i] * (1.0 - this.R[i])) / this.R[i];
        }
    }

    public static class Combinatorial {

        public static double factorial(int n) {
            if (n == 0) return 1;
            double result = 1;
            for (int i = 1; i <= n; i++) {
                result *= i;
            }
            return result;
        }

        public static double binomialCoefficient(int n, int k) {
            if (k < 0 || k > n) return 0;
            return factorial(n) / (factorial(k) * factorial(n - k));
        }
    }

    public void getMPs(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",; ");
                int[] path = new int[st.countTokens()];
                int i = 0;
                while (st.hasMoreTokens()) {
                    // Zakładamy, że w pliku są indeksy (np. 1, 2, 3), w Javie tablice są od 0.
                    // Jeśli dane w CSV są indeksowane od 1, trzeba odjąć 1.
                    // Tu zakładam surowe dane int. Dostosuj -1 jeśli w CSV są ID od 1.
                    path[i++] = Integer.parseInt(st.nextToken().trim());
                }
                MPs.add(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double[][] calculatePMF() { // Based on formula (1) - Probability Mass Function for each connection
        double[][] pmf = new double[m][];

        for (int i = 0; i < m; i++) {
            int w_i = W[i];
            pmf[i] = new double[w_i + 1];

            double r = R[i];
            double b = beta[i];

            // Obliczenie P(X >= 1) pomocniczo do stanu 0
            // Wzór (1) dla k=0: 1 - (1/beta)*(1 - (1 - r*beta)^w_i) - co wynika z sumy prawdopodobieństw
            // Ale w artykule [cite: 606] jest wprost podany wzór dla k=0.

            // Obliczamy dla k od 1 do w_i
            double sumProb = 0.0;
            for (int k = 1; k <= w_i; k++) {
                double binom = Combinatorial.binomialCoefficient(w_i, k);
                double term1 = Math.pow(r * b, k);
                double term2 = Math.pow(1.0 - r * b, w_i - k);

                pmf[i][k] = (1.0 / b) * binom * term1 * term2;
                sumProb += pmf[i][k];
            }

            // Prawdopodobieństwo dla stanu 0 to reszta z jedynki
            pmf[i][0] = 1.0 - sumProb;
        }
        return pmf;
    }

    public double[][] CDF(double[][] arPMF) {
        double[][] cdf = new double[arPMF.length][];
        for (int i = 0; i < arPMF.length; i++) {
            cdf[i] = new double[arPMF[i].length];
            double sum = 0;
            for (int k = 0; k < arPMF[i].length; k++) {
                sum += arPMF[i][k];
                cdf[i][k] = Math.min(sum, 1.0);
            }
            cdf[i][arPMF[i].length - 1] = 1.0;
        }
        return cdf;
    }

    public static double normalCDF(double x) {
        int n = 100;
        double sum = 0;

        for (int i = 0; i <= n; i++) {
            double termNumerator = Math.pow(x, 2 * i + 1);
            double termDenominator = doubleFactorial(2 * i + 1);
            sum += termNumerator / termDenominator;
        }

        double coefficient = 1.0 / Math.sqrt(2 * Math.PI);
        double expPart = Math.exp(- (x * x) / 2.0);

        return 0.5 + coefficient * expPart * sum;
    }

    // Pomocnicza metoda do silni podwójnej (n!!)
    private static double doubleFactorial(int n) {
        if (n <= 1) return 1;
        double res = 1;
        for (int i = n; i >= 1; i -= 2) {
            res *= i;
        }
        return res;
    }

    public static double normalICDF(double u) {
        if (u <= 0 || u >= 1) throw new IllegalArgumentException("u must be in (0, 1)");

        double low = -10.0;
        double high = 10.0;
        double x = 0;

        // looking for x which satisfy |normalCDF(x) - u| <= 10^-10
        while (high - low > 1e-10) {
            x = (low + high) / 2.0;
            double cdfVal = normalCDF(x);

            if (Math.abs(cdfVal - u) <= 1e-10) {
                return x;
            } else if (cdfVal < u) {
                low = x;
            } else {
                high = x;
            }
        }
        return x;
    }

    //(Fishman 12b)
    public int getWorstCaseSampleSize(double epsilon, double delta) {
        // n = ceil( [Phi^-1(1 - delta/2) / (2 * epsilon)]^2 )
        double z = normalICDF(1.0 - (delta / 2.0));
        double val = z / (2.0 * epsilon);
        return (int) Math.ceil(val * val);
    }

    // Generating random SSV (Inverse CDF Method)
    public double[][] randomSSV(int N, double[][] arCDF) {
        // Result is an array of size N x m (N vectors, each has m links)
        double[][] generatedSSVs = new double[N][m];

        for (int n = 0; n < N; n++) {
            for (int i = 0; i < m; i++) {
                double u = Math.random();

                int k = 0;
                // looking for first index k, for which CDF[i][k] >= u
                // potential usage of binary search would optimize it but for small w_i linear search is okey
                while (k < arCDF[i].length && arCDF[i][k] < u) {
                    k++;
                }

                // k  is a number of working components in link i
                // SSV holds capacity values  = k * c_i
                generatedSSVs[n][i] = k * C[i];
            }
        }
        return generatedSSVs;
    }

    // helper methods for TT agent

    // formula (3)
    public double calculateTransmissionTime(int[] path, double d, double[] ssv) {
        double cp = calculatePathCapacity(path, ssv);
        if (cp <= 0) {
            return Double.POSITIVE_INFINITY; // damaged path
        }
        int lp = calculatePathLeadTime(path);
        return lp + Math.ceil(d / cp);
    }

    // formula (4)
    public int calculatePathLeadTime(int[] path) {
        int sumL = 0;
        for (int linkIdx : path) {
            sumL += L[linkIdx];
        }
        return sumL;
    }

    // Formula (5)
    public double calculatePathCapacity(int[] path, double[] ssv) {
        double minCap = Double.MAX_VALUE;
        for (int linkIdx : path) {
            double cap = ssv[linkIdx];
            if (cap < minCap) {
                minCap = cap;
            }
        }
        return minCap;
    }
}
