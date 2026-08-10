

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Démo : glisser-déposer d'objets métier entre deux JList.
 * Chaque élément des listes est un objet Item, associé à une chaîne
 * affichée dans la liste (via toString()).
 *
 * - Glisser à l'intérieur de la même liste => PERMUTATION : l'élément
 *   déposé et l'élément cible échangent leur position (swap).
 * - Glisser d'une liste vers l'autre => déplacement classique (insertion
 *   à l'index cible + suppression dans la liste source).
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
        super("Drag & Drop d'objets entre deux JList (avec permutation)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 300);
        setLayout(new GridLayout(1, 2, 10, 10));

        DefaultListModel<Item> leftModel = new DefaultListModel<>();
        leftModel.addElement(new Item(1, "Pomme"));
        leftModel.addElement(new Item(2, "Banane"));
        leftModel.addElement(new Item(3, "Cerise"));

        DefaultListModel<Item> rightModel = new DefaultListModel<>();
        rightModel.addElement(new Item(4, "Carotte"));
        rightModel.addElement(new Item(5, "Poireau"));

        JList<Item> leftList = createList(leftModel);
        JList<Item> rightList = createList(rightModel);

        add(wrapInScrollPane(leftList, "Fruits"));
        add(wrapInScrollPane(rightList, "Légumes"));
    }

    private JList<Item> createList(DefaultListModel<Item> model) {
        JList<Item> list = new JList<>(model);
        list.setDragEnabled(true);
        // ON_OR_INSERT permet de détecter si on dépose PILE sur un élément
        // (=> permutation) ou entre deux éléments (=> insertion classique).
        list.setDropMode(DropMode.ON_OR_INSERT);
        list.setTransferHandler(new ItemTransferHandler());
        return list;
    }

    private JPanel wrapInScrollPane(JList<Item> list, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(title, SwingConstants.CENTER), BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Transferable local transportant directement une instance d'Item
     * (pas de sérialisation nécessaire, transfert intra-JVM).
     */
    private static class ItemTransferable implements Transferable {

        static final DataFlavor ITEM_FLAVOR =
                new DataFlavor(Item.class, "Item Object");

        private final Item item;

        ItemTransferable(Item item) {
            this.item = item;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { ITEM_FLAVOR };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return ITEM_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return item;
        }
    }

    /**
     * TransferHandler générique réutilisable par n'importe quelle JList<Item>.
     *
     * - Drop dans la MÊME liste : permutation (échange des deux éléments).
     * - Drop dans une AUTRE liste : déplacement classique (insertion + retrait).
     */
    private static class ItemTransferHandler extends TransferHandler {

        // Mémorise la source du drag en cours (liste + index)
        private JList<Item> sourceList;
        private int sourceIndex = -1;

        // Indique si le dernier import a déjà été géré par une permutation
        // (dans ce cas, exportDone ne doit rien retirer de plus)
        private boolean handledAsSwap = false;

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            @SuppressWarnings("unchecked")
            JList<Item> list = (JList<Item>) c;
            sourceList = list;
            sourceIndex = list.getSelectedIndex();
            handledAsSwap = false;
            Item value = list.getSelectedValue();
            return new ItemTransferable(value);
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) return false;
            if (!support.isDataFlavorSupported(ItemTransferable.ITEM_FLAVOR)) return false;
            support.setShowDropLocation(true);
            return true;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;

            Item draggedValue;
            try {
                draggedValue = (Item) support.getTransferable()
                        .getTransferData(ItemTransferable.ITEM_FLAVOR);
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
            if (dropIndex < 0) dropIndex = targetModel.getSize() - 1;
            if (dropIndex >= targetModel.getSize()) dropIndex = targetModel.getSize() - 1;

            // --- Cas 1 : même liste => PERMUTATION ---
            if (targetList == sourceList) {
                if (dropIndex == sourceIndex) {
                    handledAsSwap = false;
                    return false; // rien à faire
                }
                Item other = targetModel.getElementAt(dropIndex);
                targetModel.set(dropIndex, draggedValue);
                targetModel.set(sourceIndex, other);
                handledAsSwap = true;
                return true;
            }

            // --- Cas 2 : liste différente => déplacement au-dessus de l'élément ciblé ---
            handledAsSwap = false;
            int insertIndex = dl.getIndex();
            if (insertIndex < 0) {
                // Déposé hors de tout élément (zone vide) => on ajoute à la fin
                insertIndex = targetModel.getSize();
            }
            // On insère systématiquement AVANT l'élément ciblé, qu'on soit
            // en mode "sur l'élément" (ON) ou "entre deux éléments" (INSERT),
            // pour obtenir un comportement proche d'une permutation :
            // l'enregistrement déplacé se positionne juste au-dessus du
            // repère survolé dans l'autre liste.
            targetModel.add(insertIndex, draggedValue);
            return true;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            // La permutation a déjà tout géré : on ne retire rien de plus.
            if (!handledAsSwap && action == TransferHandler.MOVE
                    && sourceList != null && sourceIndex >= 0) {
                @SuppressWarnings("unchecked")
                DefaultListModel<Item> model =
                        (DefaultListModel<Item>) sourceList.getModel();
                if (sourceIndex < model.getSize()) {
                    model.remove(sourceIndex);
                }
            }
            sourceList = null;
            sourceIndex = -1;
            handledAsSwap = false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DualListDnD().setVisible(true));
    }
}
