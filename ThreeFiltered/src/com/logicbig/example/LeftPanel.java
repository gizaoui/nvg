package com.logicbig.example;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.function.BiPredicate;

public class LeftPanel  extends JPanel {

    private JTree tree;

    public LeftPanel() {
        this.setLayout(new BorderLayout());
        TreeNode projectHierarchyTreeNode = TradingProjectDataService.instance.getProjectHierarchy();
        tree = new JTree(projectHierarchyTreeNode);
        JTreeUtil.setTreeExpandedState(tree, true);
        TreeFilterDecorator filterDecorator = TreeFilterDecorator.decorate(tree, createUserObjectMatcher());
        tree.setCellRenderer(new TradingProjectTreeRenderer(() -> filterDecorator.getFilterField().getText()));
        this.add(new JScrollPane(tree), BorderLayout.CENTER);
        this.add(filterDecorator.getFilterField(), BorderLayout.NORTH);
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

    public JTree getTree() {
        return tree;
    }
}
