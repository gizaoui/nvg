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
    private static final UUID T05 = id("T05");
    private static final UUID T06 = id("T06");

    private static final List<Map<UUID, List<UUID>>> allTasks = List.of(
            Map.of(T01, List.of(T02)),
            Map.of(T02, List.of(T03)),
            Map.of(T03, List.of(T01)),      // cycle T01 -> T02 -> T03 -> T01
            Map.of(T04, List.of(T05)),
            Map.of(T05, List.of()),
            Map.of(T06, List.of())          // non associé à T01 ni T04
    );

    private static Map<UUID, List<UUID>> toGraph(List<Map<UUID, List<UUID>>> tasks) {
        Map<UUID, List<UUID>> graph = new LinkedHashMap<>();
        for (Map<UUID, List<UUID>> m : tasks) {
            graph.putAll(m);
        }
        return graph;
    }

    /** Liste unique (sans doublon) des nœuds de départ et de tous leurs éléments associés. */
    public static List<UUID> associated(Collection<UUID> startNodes,
                                        List<Map<UUID, List<UUID>>> tasks) {
        Map<UUID, List<UUID>> graph = toGraph(tasks);
        Set<UUID> result = new LinkedHashSet<>();
        for (UUID start : startNodes) {
            if (result.add(start)) {            // déjà présent : déjà parcouru
                collect(start, graph, result);
            }
        }
        return new ArrayList<>(result);
    }

    /** Variante pratique : associated(tasks, T01, T04). */
    public static List<UUID> associated(List<Map<UUID, List<UUID>>> tasks, UUID... startNodes) {
        return associated(Arrays.asList(startNodes), tasks);
    }

    private static void collect(UUID node, Map<UUID, List<UUID>> graph, Set<UUID> visited) {
        for (UUID child : graph.getOrDefault(node, List.of())) {
            if (visited.add(child)) {           // anti-boucle
                collect(child, graph, visited);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(associated(allTasks, T01, T04));
        // [T01, T02, T03, T04, T05]  (T06 exclu)

        System.out.println(associated(List.of(T01, T04), allTasks));  // même résultat
    }
}