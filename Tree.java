private static final List<Map<String, List<String>>> allTasks = List.of(
        Map.of("T01", List.of("T02", "T04")),
        Map.of("T02", List.of("T03")),
        Map.of("T04", List.of())
);

private record Tuple(String parent, String child) {
    @Override
    public String toString() {
        return parent + " -> " + (child != null ? child : "(none)");
    }
}

private final Set<Tuple> tuples = new LinkedHashSet<>();

public void matchTaskNode(List<Map<String, List<String>>> allTasks) {
    Map<String, List<String>> graph = new LinkedHashMap<>();
    for (Map<String, List<String>> m : allTasks) {
        graph.putAll(m);
    }
    tuples.clear();
    for (String root : graph.keySet()) {
        visit(root, graph);
    }
}

private void visit(String node, Map<String, List<String>> graph) {
    List<String> children = graph.getOrDefault(node, List.of());

    if (children.isEmpty()) {
        if (graph.containsKey(node)) {          // feuille déclarée explicitement
            Tuple t = new Tuple(node, null);
            if (tuples.add(t)) {
                System.out.println(t);
            }
        }
        return;
    }

    for (String child : children) {
        Tuple t = new Tuple(node, child);
        if (tuples.add(t)) {                    // arête nouvelle seulement
            System.out.println(t);
            visit(child, graph);                // => pas de boucle infinie
        }
    }
}