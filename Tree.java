import java.nio.charset.StandardCharsets;
import java.util.*;

public class TaskAssociations {

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
            Map.of(T04, List.of()),
            Map.of(T03, List.of(T01))
    );

    private static Map<UUID, List<UUID>> toGraph(List<Map<UUID, List<UUID>>> tasks) {
        Map<UUID, List<UUID>> graph = new LinkedHashMap<>();
        for (Map<UUID, List<UUID>> m : tasks) {
            graph.putAll(m);
        }
        return graph;
    }

    /** Liste unique (sans doublon) de node et de tous les éléments qui lui sont associés. */
    public static List<UUID> associated(UUID node, List<Map<UUID, List<UUID>>> tasks) {
        Map<UUID, List<UUID>> graph = toGraph(tasks);
        Set<UUID> result = new LinkedHashSet<>();
        result.add(node);
        collect(node, graph, result);
        return new ArrayList<>(result);
    }

    private static void collect(UUID node, Map<UUID, List<UUID>> graph, Set<UUID> visited) {
        for (UUID child : graph.getOrDefault(node, List.of())) {
            if (visited.add(child)) {       // déjà présent : on ne redescend pas (anti-boucle)
                collect(child, graph, visited);
            }
        }
    }

    public static void main(String[] args) {
        List<UUID> result = associated(T01, allTasks);
        System.out.println(result);
        // [UUID(T01), UUID(T02), UUID(T03), UUID(T04)]
    }
}