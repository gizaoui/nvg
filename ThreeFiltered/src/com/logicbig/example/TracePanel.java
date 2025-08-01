package com.logicbig.example;

import javax.swing.*;
import java.awt.*;

public class TracePanel extends JPanel {

    private JLabel trace;

    public TracePanel() {
        this.setBackground(Color.CYAN);
        trace = new JLabel("Trace");
        this.add(trace);
    }

}
