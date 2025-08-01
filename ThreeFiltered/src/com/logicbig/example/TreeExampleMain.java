package com.logicbig.example;


import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.function.BiPredicate;

public class TreeExampleMain {
    public static void main(String[] args) {

        TreeNode projectHierarchyTreeNode = TradingProjectDataService.instance.getProjectHierarchy();
        JTree tree = new JTree(projectHierarchyTreeNode);
        JTreeUtil.setTreeExpandedState(tree, true);

        TreeFilterDecorator filterDecorator = TreeFilterDecorator.decorate(tree, createUserObjectMatcher());

        tree.setCellRenderer(new TradingProjectTreeRenderer(() -> filterDecorator.getFilterField().getText()));
        JFrame frame = createFrame();
        frame.add(new JScrollPane(tree));
        frame.add(filterDecorator.getFilterField(), BorderLayout.NORTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static BiPredicate<Object, String> createUserObjectMatcher() {
        return (userObject, textToFilter) -> {
            if (userObject instanceof ProjectParticipant) {
                ProjectParticipant pp = (ProjectParticipant) userObject;
                return pp.getName().toLowerCase().contains(textToFilter)
                        || pp.getRole().toLowerCase().contains(textToFilter);
            } else if (userObject instanceof Project) {
                Project project = (Project) userObject;
                return project.getName().toLowerCase().contains(textToFilter);
            } else {
                return userObject.toString().toLowerCase().contains(textToFilter);
            }
        };
    }

    private static JFrame createFrame() {
        JFrame frame = new JFrame("JTree Filtering example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(500, 400));
        return frame;
    }
}