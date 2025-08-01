package com.logicbig.example;


import javax.swing.*;
import java.awt.*;

public class RightPanel  extends JPanel {

    private static final long serialVersionUID = 1L;

    private JLabel info;

    public RightPanel()  {
        super();
        this.setLayout(new BorderLayout());
        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(Color.WHITE);
        info = new JLabel("Selection shows here ...");
        centerPanel.add(info);

        JTextField textSouth = new JTextField();

        this.add(centerPanel, BorderLayout.CENTER);
        this.add(textSouth, BorderLayout.SOUTH);


    }

    public JLabel getInfo() {
        return info;
    }
}