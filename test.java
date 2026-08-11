import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Démo : TreeView contenant des objets métier, chaque nœud étant associé
 * à une chaîne (label) utilisée pour l'affichage et le filtrage.
 */
public class FilterableTreeViewDemo extends Application {

    // ------------------------------------------------------------------
    // 1) Le modèle : un objet métier + la chaîne associée
    // ------------------------------------------------------------------

    /**
     * Wrapper générique associant un objet métier (data) à une chaîne
     * (label) servant à l'affichage dans l'arbre et au filtrage.
     */
    static class TreeNode<T> {
        private final T data;
        private final String label;

        TreeNode(T data, String label) {
            this.data = data;
            this.label = label;
        }

        T getData() {
            return data;
        }

        String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label; // utile pour debug / fallback d'affichage
        }
    }

    /** Exemple d'objet métier arbitraire. */
    static class Produit {
        private final String nom;
        private final double prix;

        Produit(String nom, double prix) {
            this.nom = nom;
            this.prix = prix;
        }

        String getNom() {
            return nom;
        }

        double getPrix() {
            return prix;
        }
    }

    // ------------------------------------------------------------------
    // 2) TreeItem filtrable, générique sur TreeNode<T>
    // ------------------------------------------------------------------

    static class FilterableTreeItem<T> extends TreeItem<TreeNode<T>> {

        private final ObservableList<FilterableTreeItem<T>> sourceChildren = FXCollections.observableArrayList();
        private final ObjectProperty<Predicate<TreeNode<T>>> predicate = new SimpleObjectProperty<>();

        FilterableTreeItem(TreeNode<T> value) {
            super(value);
        }

        Predicate<TreeNode<T>> getPredicate() {
            return predicate.get();
        }

        void setPredicate(Predicate<TreeNode<T>> predicate) {
            this.predicate.set(predicate);
        }

        ObjectProperty<Predicate<TreeNode<T>>> predicateProperty() {
            return predicate;
        }

        void addChild(FilterableTreeItem<T> child) {
            sourceChildren.add(child);
            child.predicateProperty().bind(this.predicateProperty());
            child.predicateProperty().addListener((obs, old, np) -> child.updateFilter());
            updateFilter();
        }

        void updateFilter() {
            Predicate<TreeNode<T>> p = getPredicate();

            if (p == null) {
                super.getChildren().setAll(sourceChildren);
            } else {
                List<FilterableTreeItem<T>> visible = sourceChildren.stream()
                        .filter(child -> {
                            child.updateFilter();
                            boolean selfMatch = p.test(child.getValue());
                            boolean hasVisibleChildren = !child.getChildren().isEmpty();
                            return selfMatch || hasVisibleChildren;
                        })
                        .collect(Collectors.toList());
                super.getChildren().setAll(visible);
            }

            if (p != null && !super.getChildren().isEmpty()) {
                setExpanded(true);
            }
        }
    }

    // ------------------------------------------------------------------
    // 3) Application
    // ------------------------------------------------------------------

    @Override
    public void start(Stage stage) {
        FilterableTreeItem<Object> root = new FilterableTreeItem<>(new TreeNode<>(null, "Catalogue"));
        root.setExpanded(true);

        // --- Catégorie "Fruits" : les objets métier sont ici des Produit ---
        FilterableTreeItem<Object> fruits = new FilterableTreeItem<>(new TreeNode<>(null, "Fruits"));
        fruits.addChild(itemOf(new Produit("Pomme", 1.20)));
        fruits.addChild(itemOf(new Produit("Banane", 0.90)));
        fruits.addChild(itemOf(new Produit("Cerise", 5.50)));
        fruits.addChild(itemOf(new Produit("Kiwi", 2.10)));

        // --- Catégorie "Légumes" ---
        FilterableTreeItem<Object> legumes = new FilterableTreeItem<>(new TreeNode<>(null, "Légumes"));
        legumes.addChild(itemOf(new Produit("Carotte", 0.80)));
        legumes.addChild(itemOf(new Produit("Poireau", 1.10)));
        legumes.addChild(itemOf(new Produit("Courgette", 1.40)));

        // --- Catégorie "Viandes" avec sous-catégorie ---
        FilterableTreeItem<Object> viandes = new FilterableTreeItem<>(new TreeNode<>(null, "Viandes"));
        FilterableTreeItem<Object> volailles = new FilterableTreeItem<>(new TreeNode<>(null, "Volailles"));
        volailles.addChild(itemOf(new Produit("Poulet", 6.90)));
        volailles.addChild(itemOf(new Produit("Dinde", 7.50)));
        viandes.addChild(volailles);
        viandes.addChild(itemOf(new Produit("Bœuf", 12.00)));

        root.addChild(fruits);
        root.addChild(legumes);
        root.addChild(viandes);

        TreeView<TreeNode<Object>> treeView = new TreeView<>(root);
        treeView.setShowRoot(true);

        // Cell factory : affiche le label associé, et pourrait afficher
        // des infos supplémentaires tirées de l'objet métier (ex: prix)
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(TreeNode<Object> node, boolean empty) {
                super.updateItem(node, empty);
                if (empty || node == null) {
                    setText(null);
                } else if (node.getData() instanceof Produit produit) {
                    setText(String.format("%s (%.2f €)", produit.getNom(), produit.getPrix()));
                } else {
                    setText(node.getLabel()); // catégories : pas d'objet métier
                }
            }
        });

        // --- Champ de filtre : filtre sur le label associé à chaque nœud ---
        TextField filterField = new TextField();
        filterField.setPromptText("Filtrer...");
        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                root.setPredicate(null);
            } else {
                String lower = newVal.toLowerCase();
                root.setPredicate(node -> node.getLabel().toLowerCase().contains(lower));
            }
        });

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(10));
        layout.setTop(filterField);
        layout.setCenter(treeView);
        BorderPane.setMargin(treeView, new Insets(10, 0, 0, 0));

        stage.setScene(new Scene(layout, 420, 520));
        stage.setTitle("TreeView d'objets filtrable");
        stage.show();
    }

    /** Crée un FilterableTreeItem à partir d'un objet métier, avec son label associé. */
    private static FilterableTreeItem<Object> itemOf(Produit produit) {
        return new FilterableTreeItem<>(new TreeNode<>(produit, produit.getNom()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
