import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Démo : glisser-déposer d'objets métier entre deux JList, avec support
 * de la sélection multiple.
 *
 * Chaque élément des listes est un objet Item, associé à une chaîne
 * affichée dans la liste (via toString()).
 *
 * - On peut sélectionner plusieurs éléments (Ctrl+clic / Maj+clic) puis
 *   les glisser en bloc.
 * - Glisser dans la MÊME liste : le bloc sélectionné est retiré puis
 *   réinséré juste au-dessus de l'élément survolé (réordonnancement).
 * - Glisser dans l'AUTRE liste : le bloc sélectionné est inséré juste
 *   au-dessus de l'élément survolé, puis retiré de la liste source.
 */
public class DualListDnD extends JFrame {

    /**
     * Objet métier générique : contient une donnée (id, valeur, etc.)
     * et une chaîne d'affichage utilisée par le rendu de la JList.
     */
    public static class Item {
        private final int id;
        private final String label;

        public Item(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public int getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        // Utilisé par défaut par JList pour afficher l'élément
        @Override
        public String toString() {
            return label;
        }
    }

    public DualListDnD() {
        super("Drag & Drop multi-sélection entre deux JList");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 300);
        setLayout(new GridLayout(1, 2, 10, 10));

        DefaultListModel<Item> leftModel = new DefaultListModel<>();
        leftModel.addElement(new Item(1, "Pomme"));
        leftModel.addElement(new Item(2, "Banane"));
        leftModel.addElement(new Item(3, "Cerise"));
        leftModel.addElement(new Item(4, "Datte"));

        DefaultListModel<Item> rightModel = new DefaultListModel<>();
        rightModel.addElement(new Item(5, "Carotte"));
        rightModel.addElement(new Item(6, "Poireau"));

        // IMPORTANT : une seule instance de TransferHandler, partagée par les
        // deux listes. C'est elle qui mémorise, le temps du drag, la liste
        // source et les indices sélectionnés. Si chaque liste avait sa
        // propre instance, importData() (appelé côté liste CIBLE) ne
        // verrait jamais les indices enregistrés côté liste SOURCE, et le
        // déplacement entre les deux listes échouerait systématiquement.
        ItemTransferHandler sharedHandler = new ItemTransferHandler();

        JList<Item> leftList = createList(leftModel, sharedHandler);
        JList<Item> rightList = createList(rightModel, sharedHandler);

        add(wrapInScrollPane(leftList, "Fruits"));
        add(wrapInScrollPane(rightList, "Légumes"));
    }

    private JList<Item> createList(DefaultListModel<Item> model, ItemTransferHandler handler) {
        JList<Item> list = new JList<>(model);
        // Autorise la sélection de plusieurs éléments non contigus (Ctrl+clic)
        // ou d'une plage (Maj+clic).
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.ON_OR_INSERT);
        list.setTransferHandler(handler);
        return list;
    }

    private JPanel wrapInScrollPane(JList<Item> list, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(title, SwingConstants.CENTER), BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Transferable local transportant une liste d'objets Item
     * (pas de sérialisation nécessaire, transfert intra-JVM).
     */
    private static class ItemTransferable implements Transferable {

        static final DataFlavor ITEM_LIST_FLAVOR =
                new DataFlavor(List.class, "List of Item Objects");

        private final List<Item> items;

        ItemTransferable(List<Item> items) {
            this.items = items;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { ITEM_LIST_FLAVOR };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return ITEM_LIST_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return items;
        }
    }

    /**
     * TransferHandler générique réutilisable par n'importe quelle JList<Item>.
     * Gère le déplacement d'un ou plusieurs éléments sélectionnés :
     * - dans la même liste (réordonnancement en bloc),
     * - vers une autre liste (déplacement en bloc).
     */
    private static class ItemTransferHandler extends TransferHandler {

        // Mémorise la source du drag en cours : liste + indices sélectionnés
        // (triés par ordre croissant, comme le retourne getSelectedIndices()).
        private JList<Item> sourceList;
        private int[] sourceIndices;

        // Indique si l'import a déjà retiré les éléments de la liste source
        // (cas "même liste") pour éviter un double retrait dans exportDone.
        private boolean alreadyRemovedFromSource;

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            @SuppressWarnings("unchecked")
            JList<Item> list = (JList<Item>) c;
            sourceList = list;
            sourceIndices = list.getSelectedIndices();
            alreadyRemovedFromSource = false;
            List<Item> values = new ArrayList<>(list.getSelectedValuesList());
            return new ItemTransferable(values);
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) return false;
            if (!support.isDataFlavorSupported(ItemTransferable.ITEM_LIST_FLAVOR)) return false;
            support.setShowDropLocation(true);
            return true;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            if (sourceIndices == null || sourceIndices.length == 0) return false;

            List<Item> draggedValues;
            try {
                @SuppressWarnings("unchecked")
                List<Item> transferred = (List<Item>) support.getTransferable()
                        .getTransferData(ItemTransferable.ITEM_LIST_FLAVOR);
                draggedValues = transferred;
            } catch (UnsupportedFlavorException | IOException e) {
                return false;
            }

            @SuppressWarnings("unchecked")
            JList<Item> targetList = (JList<Item>) support.getComponent();
            @SuppressWarnings("unchecked")
            DefaultListModel<Item> targetModel =
                    (DefaultListModel<Item>) targetList.getModel();

            JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
            int dropIndex = dl.getIndex();
            if (dropIndex < 0) dropIndex = targetModel.getSize();

            if (targetList == sourceList) {
                // --- Réordonnancement dans la même liste ---

                // On ignore un dépôt qui tomberait exactement sur le bloc
                // sélectionné lui-même (rien à faire dans ce cas).
                for (int idx : sourceIndices) {
                    if (idx == dropIndex) {
                        return false;
                    }
                }

                // Combien d'éléments retirés se trouvaient AVANT le point de
                // dépôt : leur suppression va décaler l'index cible d'autant.
                int countBefore = 0;
                for (int idx : sourceIndices) {
                    if (idx < dropIndex) countBefore++;
                }

                // Retrait des éléments sélectionnés, du plus grand index au
                // plus petit, pour ne pas invalider les indices restants.
                for (int i = sourceIndices.length - 1; i >= 0; i--) {
                    targetModel.remove(sourceIndices[i]);
                }

                int insertAt = dropIndex - countBefore;
                insertAt = Math.max(0, Math.min(insertAt, targetModel.getSize()));

                for (Item item : draggedValues) {
                    targetModel.add(insertAt++, item);
                }

                alreadyRemovedFromSource = true;
                return true;
            }

            // --- Déplacement vers l'autre liste ---
            int insertAt = dropIndex;
            for (Item item : draggedValues) {
                targetModel.add(insertAt++, item);
            }
            alreadyRemovedFromSource = false;
            return true;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            if (action == TransferHandler.MOVE && !alreadyRemovedFromSource
                    && sourceList != null && sourceIndices != null) {
                @SuppressWarnings("unchecked")
                DefaultListModel<Item> model =
                        (DefaultListModel<Item>) sourceList.getModel();
                // Retrait du plus grand index au plus petit pour rester valide.
                for (int i = sourceIndices.length - 1; i >= 0; i--) {
                    int idx = sourceIndices[i];
                    if (idx < model.getSize()) {
                        model.remove(idx);
                    }
                }
            }
            sourceList = null;
            sourceIndices = null;
            alreadyRemovedFromSource = false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DualListDnD().setVisible(true));
    }
}
