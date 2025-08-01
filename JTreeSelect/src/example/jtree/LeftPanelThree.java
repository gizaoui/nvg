package example.jtree;

import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class LeftPanelThree {

	private JTree tree;
	private JScrollPane scrollTree;
	JPanel view;

	public LeftPanelThree() {
		super();

		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

		// create the child nodes
		DefaultMutableTreeNode vegetableNode = new DefaultMutableTreeNode("Vegetables");
		vegetableNode.add(new DefaultMutableTreeNode("Capsicum"));
		vegetableNode.add(new DefaultMutableTreeNode("Carrot"));
		vegetableNode.add(new DefaultMutableTreeNode("Tomato"));
		vegetableNode.add(new DefaultMutableTreeNode("Potato"));

		DefaultMutableTreeNode fruitNode = new DefaultMutableTreeNode("Fruits");
		fruitNode.add(new DefaultMutableTreeNode("Banana"));
		fruitNode.add(new DefaultMutableTreeNode("Mango"));
		fruitNode.add(new DefaultMutableTreeNode("Apple"));
		fruitNode.add(new DefaultMutableTreeNode("Grapes"));
		fruitNode.add(new DefaultMutableTreeNode("Orange"));

		// add the child nodes to the root node
		root.add(vegetableNode);
		root.add(fruitNode);

		// create the tree by passing in the root node
		tree = new JTree(root);
		scrollTree = new JScrollPane(tree);

	}

	public JPanel getView() {
		return view;
	}

	public JScrollPane getScrollTree() {
		return scrollTree;
	}

	public JTree getTree() {
		return tree;
	}

}
