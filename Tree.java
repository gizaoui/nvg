private static final List<Map<String, List<String>>> allTasks = List.of(
        Map.of("T01", List.of("T02", "T04")),
        Map.of("T02", List.of("T03")),
        Map.of("T04", List.of())
);

public void matchTaskNode(List<Map<String, List<String>>> allTasks) {
    Map<String, List<String>> graph = new LinkedHashMap<>();
    for (Map<String, List<String>> m : allTasks) {
        graph.putAll(m);
    }
    Set<String> visited = new HashSet<>();
    for (String root : graph.keySet()) {
        visit(root, graph, visited);
    }
}

private void visit(String node, Map<String, List<String>> graph, Set<String> visited) {
    if (!visited.add(node)) {
        return;                                   // nœud déjà exploré : on stoppe (anti-boucle)
    }
    List<String> children = graph.getOrDefault(node, List.of());

    if (children.isEmpty()) {
        if (graph.containsKey(node)) {            // feuille déclarée explicitement
            System.out.println(node + " -> (none)");
        }
        return;
    }
    for (String child : children) {
        System.out.println(node + " -> " + child);
        visit(child, graph, visited);
    }
}
