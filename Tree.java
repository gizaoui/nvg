import java.util.*;

record Tuple(String parent, String child) {

    @Override
    public String toString() {
        return parent + " -> " + (child != null ? child : "(none)");
    }
}

public class TreeArray {

    static String[][] build(Collection<Tuple> tuples) {
        // parent -> list of its children (empty if the parent has no child)
        Map<String, List<String>> index = new LinkedHashMap<>();
        Set<String> allChildren = new HashSet<>();

        for (Tuple t : tuples) {
            List<String> children = index.computeIfAbsent(t.parent(), k -> new ArrayList<>());
            if (t.child() != null) {
                children.add(t.child());
                allChildren.add(t.child());
            }
        }

        List<List<String>> paths = new ArrayList<>();
        for (String parent : index.keySet()) {
            if (!allChildren.contains(parent)) {   // root
                walk(parent, index, new ArrayList<>(), paths);
            }
        }

        // List<List<String>> -> String[][]
        return paths.stream()
            .map(p -> p.toArray(new String[0]))
            .toArray(String[][]::new);
    }

    // Recursive core
    static void walk(String node,
                     Map<String, List<String>> index,
                     List<String> path,
                     List<List<String>> result) {
        if (path.contains(node)) {
            throw new IllegalStateException("Cycle detected at: " + node);
        }
        path.add(node);

        List<String> children = index.get(node);
        if (children == null || children.isEmpty()) {  // leaf
            result.add(new ArrayList<>(path));
        } else {
            for (String child : children) {
                walk(child, index, path, result);
            }
        }

        path.remove(path.size() - 1);                  // backtrack
    }

    public static void main(String[] args) {
        Set<Tuple> tuples = new LinkedHashSet<>();     // duplicates impossible by design

        tuples.add(new Tuple("A", "B"));
        tuples.add(new Tuple("A", "C"));
        tuples.add(new Tuple("B", "D"));
        tuples.add(new Tuple("D", "E"));
        tuples.add(new Tuple("F", "G"));
        tuples.add(new Tuple("H", null));

        // Attempted duplicates: add() returns false and nothing is inserted
        boolean added1 = tuples.add(new Tuple("A", "B"));
        boolean added2 = tuples.add(new Tuple("H", null));
        System.out.println("Duplicate A -> B inserted? " + added1);
        System.out.println("Duplicate H -> (none) inserted? " + added2);

        System.out.println(tuples);
        System.out.println(Arrays.deepToString(build(tuples)));
    }
}