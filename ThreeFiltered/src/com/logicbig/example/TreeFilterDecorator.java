package com.logicbig.example;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.Collections;
import java.util.function.BiPredicate;

public class TreeFilterDecorator {
    private final JTree tree;
    private DefaultMutableTreeNode originalRootNode;
    private BiPredicate<Object, String> userObjectMatcher;
    private JTextField filterField;

    public TreeFilterDecorator(JTree tree, BiPredicate<Object, String> userObjectMatcher) {
        this.tree = tree;
        this.originalRootNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
        this.userObjectMatcher = userObjectMatcher;
    }

    public static TreeFilterDecorator decorate(JTree tree, BiPredicate<Object, String> userObjectMatcher) {
        TreeFilterDecorator tfd = new TreeFilterDecorator(tree, userObjectMatcher);
        tfd.init();
        return tfd;
    }

    public JTextField getFilterField() {
        return filterField;
    }

    private void init() {
        initFilterField();
    }

    private void initFilterField() {
        filterField = new JTextField();
        filterField.setMargin(new Insets(2, 5, 2, 0));
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTree();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTree();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTree();
            }
        });
    }

    private void filterTree() {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        String text = filterField.getText().trim().toLowerCase();
        if (text.equals("") && tree.getModel().getRoot() != originalRootNode) {
            model.setRoot(originalRootNode);
            JTreeUtil.setTreeExpandedState(tree, true);
        } else {
            DefaultMutableTreeNode newRootNode = matchAndBuildNode(text, originalRootNode);
            model.setRoot(newRootNode);
            JTreeUtil.setTreeExpandedState(tree, true);
        }
    }

    private DefaultMutableTreeNode matchAndBuildNode(final String text, DefaultMutableTreeNode oldNode) {
        if (!oldNode.isRoot() && userObjectMatcher.test(oldNode.getUserObject(), text)) {
            return JTreeUtil.copyNode(oldNode);
        }
        DefaultMutableTreeNode newMatchedNode = oldNode.isRoot() ? new DefaultMutableTreeNode(oldNode
                .getUserObject()) : null;
        for (TreeNode childOldNode : Collections.list(oldNode.children())) {
            DefaultMutableTreeNode newMatchedChildNode = matchAndBuildNode(text, ((DefaultMutableTreeNode) childOldNode) );
            if (newMatchedChildNode != null) {
                if (newMatchedNode == null) {
                    newMatchedNode = new DefaultMutableTreeNode(oldNode.getUserObject());
                }
                newMatchedNode.add(newMatchedChildNode);
            }
        }
        return newMatchedNode;
    }
}