# Swing

import javax.swing.*;
import java.awt.datatransfer.*;
import java.awt.*;

public class DnDExample {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Drag & Drop Demo");
        
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Pomme");
        model.addElement("Banane");
        model.addElement("Cerise");
        
        JList<String> list = new JList<>(model);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                JList<?> source = (JList<?>) c;
                return new StringSelection(source.getSelectedValue().toString());
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    String data = (String) support.getTransferable()
                            .getTransferData(DataFlavor.stringFlavor);
                    JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                    model.add(dl.getIndex(), data);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void exportDone(JComponent c, Transferable data, int action) {
                if (action == TransferHandler.MOVE) {
                    JList<?> source = (JList<?>) c;
                    model.removeElement(data);
                }
            }
        });
        
        frame.add(new JScrollPane(list));
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}



