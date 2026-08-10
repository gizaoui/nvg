# Swing

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Démo : glisser-déposer d'éléments entre deux JList.
 * - Glisser depuis une liste vers l'autre déplace l'élément.
 * - Glisser à l'intérieur de la même liste réordonne les éléments.
 */
public class DualListDnD extends JFrame {

    public DualListDnD() {
        super("Drag & Drop entre deux JList");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 300);
        setLayout(new GridLayout(1, 2, 10, 10));

        DefaultListModel<String> leftModel = new DefaultListModel<>();
        leftModel.addElement("Pomme");
        leftModel.addElement("Banane");
        leftModel.addElement("Cerise");

        DefaultListModel<String> rightModel = new DefaultListModel<>();
        rightModel.addElement("Carotte");
        rightModel.addElement("Poireau");

        JList<String> leftList = createList(leftModel);
        JList<String> rightList = createList(rightModel);

        add(wrapInScrollPane(leftList, "Fruits"));
        add(wrapInScrollPane(rightList, "Légumes"));
    }

    private JList<String> createList(DefaultListModel<String> model) {
        JList<String> list = new JList<>(model);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new ListItemTransferHandler());
        return list;
    }

    private JPanel wrapInScrollPane(JList<String> list, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(title, SwingConstants.CENTER), BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    /**
     * TransferHandler générique réutilisable par n'importe quelle JList<String>.
     * Il gère à la fois l'export (drag depuis une liste) et l'import (drop dans une liste),
     * y compris le déplacement d'une liste vers une autre.
     */
    private static class ListItemTransferHandler extends TransferHandler {

        private static final DataFlavor LOCAL_STRING_FLAVOR = DataFlavor.stringFlavor;

        // Mémorise la source du drag en cours (liste + index) pour pouvoir la retirer après le drop
        private JList<String> sourceList;
        private int sourceIndex = -1;

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            @SuppressWarnings("unchecked")
            JList<String> list = (JList<String>) c;
            sourceList = list;
            sourceIndex = list.getSelectedIndex();
            String value = list.getSelectedValue();
            return new java.awt.datatransfer.StringSelection(value);
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) return false;
            if (!support.isDataFlavorSupported(LOCAL_STRING_FLAVOR)) return false;
            support.setShowDropLocation(true);
            return true;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;

            String value;
            try {
                value = (String) support.getTransferable().getTransferData(LOCAL_STRING_FLAVOR);
            } catch (UnsupportedFlavorException | IOException e) {
                return false;
            }

            @SuppressWarnings("unchecked")
            JList<String> targetList = (JList<String>) support.getComponent();
            DefaultListModel<String> targetModel =
                    (DefaultListModel<String>) targetList.getModel();

            JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
            int dropIndex = dl.getIndex();
            if (dropIndex < 0) dropIndex = targetModel.getSize();

            // Ajustement si on déplace un élément vers le bas dans la même liste
            if (targetList == sourceList && sourceIndex >= 0 && sourceIndex < dropIndex) {
                dropIndex--;
            }

            targetModel.add(dropIndex, value);
            return true;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            if (action == TransferHandler.MOVE && sourceList != null && sourceIndex >= 0) {
                DefaultListModel<String> model =
                        (DefaultListModel<String>) sourceList.getModel();
                // On ne retire que si l'élément est toujours à l'index d'origine avec la même valeur
                if (sourceIndex < model.getSize()) {
                    model.remove(sourceIndex);
                }
            }
            sourceList = null;
            sourceIndex = -1;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DualListDnD().setVisible(true));
    }
}







                