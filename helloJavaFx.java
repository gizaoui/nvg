<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.Button?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.ListView?>
<?import javafx.scene.control.SplitPane?>
<?import javafx.scene.control.TextArea?>
<?import javafx.scene.layout.AnchorPane?>

<SplitPane dividerPositions="0.35" prefHeight="400.0" prefWidth="600.0" xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml">
   <items>
      <!-- Panneau 1 : Navigation / Liste (à gauche) -->
      <AnchorPane minWidth="150.0">
         <children>
            <Label text="Menu / Navigation" AnchorPane.leftAnchor="15.0" AnchorPane.topAnchor="15.0" />
            
            <ListView AnchorPane.bottomAnchor="15.0" AnchorPane.leftAnchor="15.0" AnchorPane.rightAnchor="15.0" AnchorPane.topAnchor="45.0" />
         </children>
      </AnchorPane>

      <!-- Panneau 2 : Contenu principal (à droite) -->
      <AnchorPane minWidth="200.0">
         <children>
            <Label text="Détails du document" AnchorPane.leftAnchor="15.0" AnchorPane.topAnchor="15.0" />
            
            <!-- Zone de texte redimensionnable qui remplit le centre -->
            <TextArea promptText="Écrivez vos notes ici..." AnchorPane.bottomAnchor="50.0" AnchorPane.leftAnchor="15.0" AnchorPane.rightAnchor="15.0" AnchorPane.topAnchor="45.0" />
            
            <!-- Bouton d'action ancré en bas à droite -->
            <Button text="Sauvegarder" AnchorPane.bottomAnchor="15.0" AnchorPane.rightAnchor="15.0" />
         </children>
      </AnchorPane>
   </items>
</SplitPane>
