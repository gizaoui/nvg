import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import java.util.ArrayList;
import java.util.List;

public class TreeUtils {

    // Main method to call
    public static <T> List<TreeItem<T>> getAllItems(TreeView<T> treeView) {
        List<TreeItem<T>> allItems = new ArrayList<>();
        if (treeView.getRoot() != null) {
            walkTree(treeView.getRoot(), allItems);
        }
        return allItems;
    }List<TreeItem<String>> flatList = TreeUtils.getAllItems(myTreeView);
flatList.forEach(item -> System.out.println(item.getValue()));


    /
    private static <T> void walkTree(TreeItem<T> currentItem, List<TreeItem<T>> list) {
        list.add(currentItem); // Add current node
        
        // Recursively add all children
        for (TreeItem<T> child : currentItem.getChildren()) {
            walkTree(child, list);
        }
    }
}


List<TreeItem<String>> flatList = TreeUtils.getAllItems(myTreeView);
flatList.forEach(item -> System.out.println(item.getValue()));


