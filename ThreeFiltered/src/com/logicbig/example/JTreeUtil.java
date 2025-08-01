package com.logicbig.example;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JTreeUtil {

    public static void setTreeExpandedState(JTree tree, boolean expanded) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getModel().getRoot();
        setNodeExpandedState(tree, node, expanded);
    }

    public static void setNodeExpandedState(JTree tree, DefaultMutableTreeNode node, boolean expanded) {
        for (TreeNode treeNode : Collections.list(node.children())) {
            setNodeExpandedState(tree, (DefaultMutableTreeNode) treeNode, expanded);
        }
        if (!expanded && node.isRoot()) {
            return;
        }
        TreePath path = new TreePath(node.getPath());
        if (expanded) {
            tree.expandPath(path);
        } else {
            tree.collapsePath(path);
        }
    }

    public static DefaultMutableTreeNode copyNode(DefaultMutableTreeNode oldNode) {
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(oldNode.getUserObject());
        for (TreeNode oldChildNode : Collections.list(oldNode.children()) ) {
            DefaultMutableTreeNode newChildNode = new DefaultMutableTreeNode(((DefaultMutableTreeNode) oldChildNode).getUserObject());
            newNode.add(newChildNode);
            if (!oldChildNode.isLeaf()) {
                copyChildrenTo((DefaultMutableTreeNode) oldChildNode, newChildNode);
            }
        }
        return newNode;
    }

    public static void copyChildrenTo(DefaultMutableTreeNode from, DefaultMutableTreeNode to) {
        for (TreeNode oldChildNode : Collections.list(from.children())) {
            DefaultMutableTreeNode newChildNode = new DefaultMutableTreeNode(((DefaultMutableTreeNode) oldChildNode).getUserObject());
            to.add(newChildNode);
            if (!oldChildNode.isLeaf()) {
                copyChildrenTo((DefaultMutableTreeNode) oldChildNode, newChildNode);
            }
        }
    }
}