package com.logicbig.example;

import com.logicbig.example.entity.Project;
import com.logicbig.example.entity.ProjectParticipant;
import com.logicbig.util.RandomUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public enum TradingProjectDataService {

    instance;


    private DefaultMutableTreeNode rootNode;

    TradingProjectDataService() {
        rootNode = new DefaultMutableTreeNode("Trading Project Modules");

        addModule("Trading", "Real Time Trading", "Order System");
        addModule("Future/Option", "Option Analyzer", "Market Scanning System");
        addModule("Fixed Income", "Bond Tool", "Price/Yield Calculator",
                "Strategy Evaluator");
    }

    private void addModule(String module, String... projects) {
        DefaultMutableTreeNode moduleNode = new DefaultMutableTreeNode(module);
        rootNode.add(moduleNode);
        for (String project : projects) {
            moduleNode.add(getProjectNode(project));
        }
    }

    private MutableTreeNode getProjectNode(String project) {
        DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(new Project(project));
        String ROLES[] = {"Project Manager", "Tech Lead", "Developer", "Scrum Master", "Business Analyst"};
        for (int i = 0; i < ROLES.length; i++) {
            projectNode.add(getEmployeeNodeForRole(ROLES[i]));
        }
        return projectNode;
    }

    private MutableTreeNode getEmployeeNodeForRole(String role) {
        ProjectParticipant projectParticipant = new ProjectParticipant(RandomUtil.getFullName(), role);
        return new DefaultMutableTreeNode(projectParticipant);
    }

    public TreeNode getProjectHierarchy() {
        return rootNode;
    }
}