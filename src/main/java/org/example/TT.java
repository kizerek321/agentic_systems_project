package org.example;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class TT extends Agent {
    private double d;
    private double T;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length == 2) {
            try {
                d = Double.parseDouble(args[0].toString());
                T = Double.parseDouble(args[1].toString());
            } catch (NumberFormatException e) {
                System.err.println("TT Agent: not correct format of arguments d and T.");
                doDelete();
                return;
            }
        } else {
            System.err.println("TT Agent: lack of arguments (d, T).");
            doDelete();
            return;
        }

        System.out.println("Hallo! Transmission times computing-agent " + getAID().getName() + " is ready.");
        System.out.println("The aim is to estimate the probability of sending " + d + " units of flow within time " + T);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("transmission-time-estimation");
        sd.setName("JADE-reliability");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new CalculateReliabilityBehaviour());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private class CalculateReliabilityBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                try {
                    SimulationData data = (SimulationData) msg.getContentObject();

                    saveSSVsToCSV(data.generatedSSVs, "SSVs.csv");

                    MFN mfn = new MFN(data.m, data.W, data.C, data.L, data.R, data.rho);
                    mfn.getMPs(data.mpsFilePath);

                    int successCount = 0;
                    int N = data.generatedSSVs.length;

                    // Iterating on each vector ( Monte Carlo)
                    for (double[] ssv : data.generatedSSVs) {
                        // for each state we look for shortest path
                        double minTime = Double.POSITIVE_INFINITY;

                        // checking each minimal path
                        for (int[] path : mfn.MPs) {
                            // calculating transmision time for path in this state
                            double time = mfn.calculateTransmissionTime(path, d, ssv);
                            if (time < minTime) {
                                minTime = time;
                            }
                        }

                        // if shortest time is smaller than T -> success
                        if (minTime <= T) {
                            successCount++;
                        }
                    }

                    double reliability = (double) successCount / N;
                    System.out.println("Estimated network reliability is equal to " + reliability);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(String.valueOf(reliability));
                    send(reply);

                    // decide if agent need to stay active or should be deleted after performing task
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }

    private void saveSSVsToCSV(double[][] ssvs, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (double[] vector : ssvs) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < vector.length; i++) {
                    sb.append(vector[i]);
                    if (i < vector.length - 1) {
                        sb.append(",");
                    }
                }
                writer.write(sb.toString());
                writer.newLine();
            }
             System.out.println("SSVs saved to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}