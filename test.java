import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Démo : TreeView contenant des objets métier, filtrable via un TextField,
 * avec une TableView à droite recevant des Produit par drag and drop.
 *
 * Compatible Java 8.
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

    /** Référence à l'objet en cours de drag, car Produit n'est pas Serializable. */
    private Produit draggedProduit;

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

        // --- TableView à droite, cible du drop ---
        TableView<Produit> tableView = createTableView();

        // Cell factory : affichage + source du drag
        treeView.setCellFactory(tv -> {
            TreeCell<TreeNode<Object>> cell = new TreeCell<TreeNode<Object>>() {
                @Override
                protected void updateItem(TreeNode<Object> node, boolean empty) {
                    super.updateItem(node, empty);
                    if (empty || node == null) {
                        setText(null);
                    } else if (node.getData() instanceof Produit) {
                        Produit produit = (Produit) node.getData();
                        setText(String.format("%s (%.2f €)", produit.getNom(), produit.getPrix()));
                    } else {
                        setText(node.getLabel()); // catégories : pas d'objet métier
                    }
                }
            };

            // --- Démarrage du drag : uniquement pour les feuilles portant un Produit ---
            cell.setOnDragDetected(event -> {
                TreeNode<Object> node = cell.getItem();
                if (node != null && node.getData() instanceof Produit) {
                    draggedProduit = (Produit) node.getData();

                    Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    // Le contenu texte est nécessaire pour initier le drag sur toutes plateformes,
                    // même si on récupère l'objet réel via draggedProduit au moment du drop.
                    content.putString(node.getLabel());
                    db.setContent(content);
                    event.consume();
                }
            });

            return cell;
        });

        // --- Champ de filtre : filtre sur le label associé à chaque nœud ---
        TextField filterField = new TextField();
        filterField.setPromptText("Filtrer...");
        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                root.setPredicate(null);
            } else {
                String lower = newVal.toLowerCase();
                root.setPredicate(node -> node.getLabel().toLowerCase().contains(lower));
            }
        });

        SplitPane splitPane = new SplitPane(treeView, tableView);
        splitPane.setDividerPositions(0.45);

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(10));
        layout.setTop(filterField);
        layout.setCenter(splitPane);
        BorderPane.setMargin(splitPane, new Insets(10, 0, 0, 0));

        stage.setScene(new Scene(layout, 700, 520));
        stage.setTitle("TreeView filtrable -> TableView (drag and drop)");
        stage.show();
    }

    /** Construit la TableView cible, avec gestion du drop de Produit. */
    private TableView<Produit> createTableView() {
        TableView<Produit> tableView = new TableView<>();
        tableView.setPlaceholder(new javafx.scene.control.Label("Glissez des produits ici"));

        TableColumn<Produit, String> nomCol = new TableColumn<>("Nom");
        nomCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNom()));
        nomCol.setPrefWidth(150);

        TableColumn<Produit, Number> prixCol = new TableColumn<>("Prix (€)");
        prixCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPrix()));
        prixCol.setPrefWidth(100);

        tableView.getColumns().add(nomCol);
        tableView.getColumns().add(prixCol);

        // --- Acceptation du survol pendant le drag ---
        tableView.setOnDragOver((DragEvent event) -> {
            if (event.getGestureSource() != tableView && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // --- Dépôt effectif : ajout du Produit dans la table ---
        tableView.setOnDragDropped((DragEvent event) -> {
            boolean success = false;
            if (draggedProduit != null) {
                if (!tableView.getItems().contains(draggedProduit)) {
                    tableView.getItems().add(draggedProduit);
                }
                success = true;
            }
            event.setDropCompleted(success);
            draggedProduit = null;
            event.consume();
        });

        return tableView;
    }

    /** Crée un FilterableTreeItem à partir d'un objet métier, avec son label associé. */
    private static FilterableTreeItem<Object> itemOf(Produit produit) {
        return new FilterableTreeItem<>(new TreeNode<>(produit, produit.getNom()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
