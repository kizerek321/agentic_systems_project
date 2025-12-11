package org.example;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.util.Arrays;

public class SSVGenerator extends Agent {
    private SSVGeneratorGui gui;
    private int N;
    private double epsilon;
    private double delta;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length == 2) {
            try {
                epsilon = Double.parseDouble(args[0].toString());
                delta = Double.parseDouble(args[1].toString());

                if (epsilon <= 0 || epsilon >= 1 || delta <= 0 || delta >= 1) {
                    System.err.println("Epsilon and Delta must be in range (0, 1).");
                    doDelete();
                    return;
                }
            } catch (NumberFormatException e) {
                System.err.println("arguments' format error");
                doDelete();
                return;
            }
        } else {
            System.err.println("lack of delta and epsilon");
            doDelete();
            return;
        }

        //Calculating N based on formula 12b from Fishman using normalICDF from MNF class
        double z = MFN.normalICDF(1.0 - (delta / 2.0));
        double val = z / (2.0 * epsilon);
        this.N = (int) Math.ceil(val * val);

        System.out.println("Hallo! SSVGenerator-agent " + getAID().getName() + " is ready.");
        System.out.println("The minimum number of iterations is equal to " + N);

        gui = new SSVGeneratorGui(this);
        gui.setVisible(true);

        addBehaviour(new ReceiveReliabilityBehaviour());
    }

    @Override
    protected void takeDown() {
        if (gui != null) {
            gui.dispose();
        }
        System.out.println("SSVGenerator-agent " + getAID().getName() + " terminating.");
    }

    public void startSimulation(String mpsFilePath, int m, int[] W, double[] C, int[] L, double[] R, double[] rho) {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println("It has been created the MFN with the following parameters:");
                System.out.println("W=" + Arrays.toString(W));
                System.out.println("C=" + Arrays.toString(C));
                System.out.println("L=" + Arrays.toString(L));
                System.out.println("R=" + Arrays.toString(R));
                System.out.println("rho=" + Arrays.toString(rho));

                MFN mfn = new MFN(m, W, C, L, R, rho);
                mfn.getMPs(mpsFilePath);

                double[][] pmf = mfn.calculatePMF();
                double[][] cdf = mfn.CDF(pmf);
                double[][] generatedSSVs = mfn.randomSSV(N, cdf);

                System.out.println(N + " random SSVs have been generated!");

                AID ttAgent = findTTAgent();
                if (ttAgent == null) {
                    System.out.println("TT not found");
                    return;
                }

                System.out.println("Found the following transmission times computing agent:\n" + ttAgent.getName());

                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(ttAgent);

                try {
                    SimulationData data = new SimulationData(m, W, C, L, R, rho, mpsFilePath, generatedSSVs);
                    msg.setContentObject(data);
                    send(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private AID findTTAgent() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("transmission-time-estimation");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                return result[0].getName();
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return null;
    }

    private class ReceiveReliabilityBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                System.out.println("Estimated network reliability is equal to " + msg.getContent());
                doDelete();
            } else {
                block();
            }
        }
    }
}
