import java.util.*;

record Tuple(UUID parent, UUID child) {

    Tuple {
        Objects.requireNonNull(parent, "parent must not be null");
    }

    @Override
    public String toString() {
        return parent + " -> " + (child != null ? child : "(none)");
    }
}

public class TreeArray {

    static UUID[][] build(Collection<Tuple> tuples) {
        // parent -> list of its children (empty if the parent has no child)
        Map<UUID, List<UUID>> index = new LinkedHashMap<>();
        Set<UUID> allChildren = new HashSet<>();

        for (Tuple t : tuples) {
            List<UUID> children = index.computeIfAbsent(t.parent(), k -> new ArrayList<>());
            if (t.child() != null) {
                children.add(t.child());
                allChildren.add(t.child());
            }
        }

        List<List<UUID>> paths = new ArrayList<>();
        for (UUID parent : index.keySet()) {
            if (!allChildren.contains(parent)) {   // root
                walk(parent, index, new ArrayList<>(), paths);
            }
        }

        // List<List<UUID>> -> UUID[][]
        return paths.stream()
            .map(p -> p.toArray(new UUID[0]))
            .toArray(UUID[][]::new);
    }

    // Recursive core
    static void walk(UUID node,
                     Map<UUID, List<UUID>> index,
                     List<UUID> path,
                     List<List<UUID>> result) {
        if (path.contains(node)) {
            throw new IllegalStateException("Cycle detected at: " + node);
        }
        path.add(node);

        List<UUID> children = index.get(node);
        if (children == null || children.isEmpty()) {  // leaf
            result.add(new ArrayList<>(path));
        } else {
            for (UUID child : children) {
                walk(child, index, path, result);
            }
        }

        path.remove(path.size() - 1);                  // backtrack
    }

    // Deterministic UUID for the demo (use UUID.randomUUID() in real code)
    static UUID id(long n) {
        return new UUID(0L, n);
    }

    public static void main(String[] args) {
        UUID a = id(1), b = id(2), c = id(3), d = id(4),
             e = id(5), f = id(6), g = id(7), h = id(8);

        Set<Tuple> tuples = new LinkedHashSet<>();     // duplicates impossible by design

        tuples.add(new Tuple(a, b));
        tuples.add(new Tuple(a, c));
        tuples.add(new Tuple(b, d));
        tuples.add(new Tuple(d, e));
        tuples.add(new Tuple(f, g));
        tuples.add(new Tuple(h, null));

        // Attempted duplicates: add() returns false and nothing is inserted
        System.out.println("Duplicate a -> b inserted? " + tuples.add(new Tuple(a, b)));
        System.out.println("Duplicate h -> (none) inserted? " + tuples.add(new Tuple(h, null)));

        System.out.println(tuples.size() + " tuples");
        for (UUID[] path : build(tuples)) {
            System.out.println(Arrays.toString(path));
        }
    }
}