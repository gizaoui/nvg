import java.nio.charset.StandardCharsets;
import java.util.*;

public class TaskPaths {

    private static UUID id(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    private static final UUID T01 = id("T01");
    private static final UUID T02 = id("T02");
    private static final UUID T03 = id("T03");
    private static final UUID T04 = id("T04");

    private static final List<Map<UUID, List<UUID>>> allTasks = List.of(
            Map.of(T01, List.of(T02, T04)),
            Map.of(T02, List.of(T03)),
            Map.of(T04, List.of())
    );

    public static UUID[][] findPaths(List<Map<UUID, List<UUID>>> allTasks) {
        Map<UUID, List<UUID>> graph = new LinkedHashMap<>();
        for (Map<UUID, List<UUID>> m : allTasks) {
            graph.putAll(m);
        }

        // racines = nœuds qui ne sont l'enfant de personne
        Set<UUID> allChildren = new HashSet<>();
        graph.values().forEach(allChildren::addAll);

        List<List<UUID>> result = new ArrayList<>();
        for (UUID root : graph.keySet()) {
            if (!allChildren.contains(root)) {
                walk(root, graph, new ArrayList<>(), result);
            }
        }

        return result.stream()
                .map(p -> p.toArray(new UUID[0]))
                .toArray(UUID[][]::new);
    }

    private static void walk(UUID node, Map<UUID, List<UUID>> graph,
                             List<UUID> path, List<List<UUID>> result) {
        path.add(node);

        boolean extended = false;
        for (UUID child : graph.getOrDefault(node, List.of())) {
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
        UUID[][] paths = findPaths(allTasks);
        System.out.println(Arrays.deepToString(paths));
    }
}
