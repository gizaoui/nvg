package com.logicbig.example;


import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

public class TreeExampleMain {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Select a Project");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(1500, 800));

        LeftPanel leftPanel = new LeftPanel();
        leftPanel.setPreferredSize(new Dimension(400, 800));

        RightPanel rightPanel =  new RightPanel();

        leftPanel.getTree().getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) leftPanel.getTree().getLastSelectedPathComponent();
                try {
                    frame.setTitle(selectedNode.getUserObject().toString());
                    rightPanel.getInfo().setText(selectedNode.getUserObject().toString());
                }
                catch (Exception ex) {
                    frame.setTitle("Select a Project");
                }

            }
        });

        JTextArea textAreaEast = new JTextArea(1,30);
        textAreaEast.setBackground(Color.RED);

        TracePanel southPanel =  new TracePanel();


        JSplitPane plitPaneNorth = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        plitPaneNorth.setPreferredSize(new Dimension(1500, 600));

        JSplitPane plitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, plitPaneNorth, southPanel);





        frame.add(plitPane2);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    }


}