package com.logicbig.example;


import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.function.BiPredicate;

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



        JSplitPane plitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);


        plitPane.add(leftPanel);
        plitPane.add(rightPanel);


        frame.add(plitPane);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    }


}