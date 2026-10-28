static <T> Stream<TreeItem<T>> flatten(TreeItem<T> item) {
    return Stream.concat(Stream.of(item),
        item.getChildren().stream().flatMap(c -> flatten(c)));
}

flatten(treeView.getRoot())
    .map(TreeItem::getValue)
    .forEach(System.out::println);