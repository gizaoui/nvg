dialog.initOwner(stage);

dialog.setOnShown(e -> {
    dialog.setX(stage.getX() + (stage.getWidth() - dialog.getWidth()) / 2);
    dialog.setY(stage.getY() + (stage.getHeight() - dialog.getHeight()) / 2);
});