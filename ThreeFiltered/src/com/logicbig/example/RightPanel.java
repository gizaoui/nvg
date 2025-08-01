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

        JPanel componentPanel = new JPanel();
        componentPanel.setLayout(new BorderLayout());
        componentPanel.add(new JLabel(" Component : "), BorderLayout.WEST);
        JTextField textSouth = new JTextField();
        textSouth.setMargin(new Insets(2, 5, 2, 0));
        componentPanel.add(textSouth, BorderLayout.CENTER);

        this.add(centerPanel, BorderLayout.CENTER);
        this.add(componentPanel, BorderLayout.SOUTH);
    }

    public JLabel getInfo() {
        return info;
    }
}