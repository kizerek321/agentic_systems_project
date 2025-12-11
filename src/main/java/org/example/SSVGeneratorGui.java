package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.StringTokenizer;

public class SSVGeneratorGui extends JFrame {
    private SSVGenerator myAgent;

    private JTextField linksField, wField, cField, lField, rField, rhoField, fileField;
    private JButton openButton, sendButton;

    public SSVGeneratorGui(SSVGenerator agent) {
        super("SSVGenerator GUI");
        this.myAgent = agent;

        initializeComponents();

        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        setLayout(new GridLayout(9, 2, 5, 5));

        add(new JLabel("The number of links (m):"));
        linksField = new JTextField("5");
        add(linksField);

        add(new JLabel("File Name:"));
        JPanel filePanel = new JPanel(new BorderLayout());
        fileField = new JTextField();
        openButton = new JButton("Open");
        filePanel.add(fileField, BorderLayout.CENTER);
        filePanel.add(openButton, BorderLayout.EAST);
        add(filePanel);

        add(new JLabel("The component numbers vector (W):"));
        wField = new JTextField("4, 3, 2, 3, 2");
        add(wField);

        add(new JLabel("The component capacities vector (C):"));
        cField = new JTextField("10, 15, 25, 15, 20");
        add(cField);

        add(new JLabel("The lead time vector (L):"));
        lField = new JTextField("5, 7, 6, 5, 8");
        add(lField);

        add(new JLabel("The component reliabilities vector (R):"));
        rField = new JTextField("0.7, 0.65, 0.67, 0.71, 0.75");
        add(rField);

        add(new JLabel("The vector of correlation (rho):"));
        rhoField = new JTextField("0.1, 0.3, 0.5, 0.7, 0.9");
        add(rhoField);

        add(new JLabel(""));
        sendButton = new JButton("Send Data");
        add(sendButton);

        openButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("."));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                fileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int m = Integer.parseInt(linksField.getText().trim());
                    String filePath = fileField.getText().trim();

                    if (filePath.isEmpty()) {
                        JOptionPane.showMessageDialog(SSVGeneratorGui.this, "Please select a file!", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int[] W = parseIntArray(wField.getText(), m);
                    double[] C = parseDoubleArray(cField.getText(), m);
                    int[] L = parseIntArray(lField.getText(), m);
                    double[] R = parseDoubleArray(rField.getText(), m);
                    double[] rho = parseDoubleArray(rhoField.getText(), m);

                    myAgent.startSimulation(filePath, m, W, C, L, R, rho);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SSVGeneratorGui.this, "Invalid data format: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });
    }

    private int[] parseIntArray(String text, int expectedSize) {
        StringTokenizer st = new StringTokenizer(text, ",; ");
        int[] arr = new int[st.countTokens()];
        if (arr.length != expectedSize) throw new IllegalArgumentException("Vector size mismatch. Expected " + expectedSize + ", got " + arr.length);
        for (int i = 0; i < arr.length; i++) arr[i] = Integer.parseInt(st.nextToken());
        return arr;
    }

    private double[] parseDoubleArray(String text, int expectedSize) {
        StringTokenizer st = new StringTokenizer(text, ",; ");
        double[] arr = new double[st.countTokens()];
        if (arr.length != expectedSize) throw new IllegalArgumentException("Vector size mismatch. Expected " + expectedSize + ", got " + arr.length);
        for (int i = 0; i < arr.length; i++) arr[i] = Double.parseDouble(st.nextToken());
        return arr;
    }
}