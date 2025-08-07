package com.logicbig.example;

import com.logicbig.example.entity.Project;
import com.logicbig.example.entity.ProjectParticipant;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.function.Supplier;

public class TradingProjectTreeRenderer extends DefaultTreeCellRenderer {

    private static final String SPAN_FORMAT = "<span style='color:%s;'>%s</span>";
    private Supplier<String> filterTextSupplier;

    public TradingProjectTreeRenderer(Supplier<String> filterTextSupplier) {
        this.filterTextSupplier = filterTextSupplier;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();


        if (userObject instanceof ProjectParticipant) {
            ProjectParticipant pp = (ProjectParticipant) userObject;
            String text = String.format(SPAN_FORMAT, "rg b(0, 0, 150)", renderFilterMatch(node, pp.getName()));
            text += " [" + String.format(SPAN_FORMAT, "rgb(90, 70, 0)", renderFilterMatch(node, pp.getRole())) + "]";
            this.setText("<html>" + text + "</html>");

        } else if (userObject instanceof Project) {
            Project project = (Project) userObject;
            String text = String.format(SPAN_FORMAT, "rgb(0,70,0)", renderFilterMatch(node, project.getName()));
            this.setText("<html>" + text + "</html>");

        } else {
            String text = String.format(SPAN_FORMAT, "rgb(120,0,0)", renderFilterMatch(node, userObject.toString()));
            this.setText("<html>" + text + "</html>");
        }

        return this;
    }

    private String renderFilterMatch(DefaultMutableTreeNode node, String text) {
        if (node.isRoot()) {
            return text;
        }
        String textToFilter = filterTextSupplier.get();
        return HtmlHighlighter.highlightText(text, textToFilter);
    }
}