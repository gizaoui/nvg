private static final List<Map<String, List<String>>> allTasks = List.of(
        Map.of("T01", List.of("T02", "T04")),
        Map.of("T02", List.of("T03")),
        Map.of("T04", List.of())
);

public static List<List<String>> buildPaths(List<Map<String, List<String>>> tasks) {
    Map<String, List<String>> graph = new LinkedHashMap<>();
    Set<String> childIds = new HashSet<>();

    for (Map<String, List<String>> m : tasks) {
        m.forEach((parent, children) -> {
            graph.computeIfAbsent(parent, k -> new ArrayList<>()).addAll(children);
            childIds.addAll(children);
        });
    }

    List<List<String>> paths = new ArrayList<>();
    for (String root : graph.keySet()) {
        if (!childIds.contains(root)) {
            collect(root, graph, new ArrayList<>(), paths);
        }
    }
    return paths;
}

private static void collect(String id, Map<String, List<String>> graph,
                            List<String> path, List<List<String>> paths) {
    if (path.contains(id)) {
        throw new IllegalStateException("Cycle détecté : " + path + " -> " + id);
    }
    path.add(id);

    List<String> children = graph.getOrDefault(id, List.of());
    if (children.isEmpty()) {
        paths.add(new ArrayList<>(path));
    } else {
        for (String child : children) {
            collect(child, graph, path, paths);
        }
    }
    path.remove(path.size() - 1);
}

public static void main(String[] args) {
    List<List<String>> paths = buildPaths(allTasks);
    System.out.println(paths);
}