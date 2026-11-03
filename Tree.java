import java.util.*;

public class TaskPaths {

    private static final List<Map<String, List<String>>> allTasks = List.of(
            Map.of("T01", List.of("T02", "T04")),
            Map.of("T02", List.of("T03")),
            Map.of("T04", List.of())
    );

    public static String[][] findPaths(List<Map<String, List<String>>> allTasks) {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (Map<String, List<String>> m : allTasks) {
            graph.putAll(m);
        }

        // racines = nœuds qui ne sont l'enfant de personne
        Set<String> allChildren = new HashSet<>();
        graph.values().forEach(allChildren::addAll);

        List<List<String>> result = new ArrayList<>();
        for (String root : graph.keySet()) {
            if (!allChildren.contains(root)) {
                walk(root, graph, new ArrayList<>(), result);
            }
        }

        return result.stream()
                .map(p -> p.toArray(new String[0]))
                .toArray(String[][]::new);
    }

    private static void walk(String node, Map<String, List<String>> graph,
                             List<String> path, List<List<String>> result) {
        path.add(node);

        boolean extended = false;
        for (String child : graph.getOrDefault(node, List.of())) {
            if (path.contains(child)) {
                continue;                   // cycle : on ne suit pas cette branche
            }
            extended = true;
            walk(child, graph, path, result);
        }
        if (!extended) {                    // feuille (ou impasse due à un cycle)
            result.add(new ArrayList<>(path));
        }

        path.remove(path.size() - 1);       // backtracking
    }

    public static void main(String[] args) {
        String[][] paths = findPaths(allTasks);
        System.out.println(Arrays.deepToString(paths));
        // [[T01, T02, T03], [T01, T04]]
    }
}