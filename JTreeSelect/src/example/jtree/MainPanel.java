package example.jtree;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class MainPanel extends JFrame {

	private static final long serialVersionUID = 1L;

	public MainPanel() throws HeadlessException {
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("JTree Selection example");
		// this.setSize(1500, 1000);
		// this.setLocationRelativeTo(null);
		this.setLayout(new BorderLayout());
		this.add(this.getSplitPane(), BorderLayout.CENTER);
	}

	private JSplitPane getSplitPane() {

		TreeNode projectHierarchyTreeNode = TradingProjectDataService.instance.getProjectHierarchy();
		JTree tree = new JTree(projectHierarchyTreeNode);
		JTreeUtil.setTreeExpandedState(tree, true);

		JScrollPane leftPanel = new JScrollPane(tree);
		// leftPanel.setPreferredSize(new Dimension(400, 700));

		RightPanelThree rightPanel = getRightPanelContainer();
		// rightPanel.setPreferredSize(new Dimension(700, 700));

		tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				rightPanel.getInfo().setText(selectedNode.getUserObject().toString());
			}
		});

		JSplitPane plitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		//plitPane.setDividerLocation(0.5);
		plitPane.add(leftPanel);
		plitPane.add(rightPanel);

		return plitPane;
	}

	private LeftPanelThree getLeftPanelContainer() {
		return new LeftPanelThree();
	}

	private RightPanelThree getRightPanelContainer() {
		return new RightPanelThree();
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				MainPanel app = new MainPanel();
				app.setVisible(true);
				app.pack();
			}
		});
	}
}
