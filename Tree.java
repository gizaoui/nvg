import java.util.*;

record Tuple(String parent, String enfant) {}

public class ArbreTableau {

    static String[][] construire(List<Tuple> tuples) {
        // parent -> liste de ses enfants (vide si le parent n'a pas d'enfant)
        Map<String, List<String>> index = new LinkedHashMap<>();
        Set<String> enfants = new HashSet<>();

        for (Tuple t : tuples) {
            List<String> fils = index.computeIfAbsent(t.parent(), k -> new ArrayList<>());
            if (t.enfant() != null) {
                fils.add(t.enfant());
                enfants.add(t.enfant());
            }
        }

        List<List<String>> chemins = new ArrayList<>();
        for (String parent : index.keySet()) {
            if (!enfants.contains(parent)) {   // racine
                parcourir(parent, index, new ArrayList<>(), chemins);
            }
        }

        // List<List<String>> -> String[][]
        return chemins.stream()
            .map(c -> c.toArray(new String[0]))
            .toArray(String[][]::new);
    }

    // Cœur récursif
    static void parcourir(String noeud,
                          Map<String, List<String>> index,
                          List<String> chemin,
                          List<List<String>> resultat) {
        if (chemin.contains(noeud)) {
            throw new IllegalStateException("Cycle détecté sur : " + noeud);
        }
        chemin.add(noeud);

        List<String> fils = index.get(noeud);
        if (fils == null || fils.isEmpty()) {  // feuille : pas d'entrée ou aucun enfant
            resultat.add(new ArrayList<>(chemin));
        } else {
            for (String f : fils) {
                parcourir(f, index, chemin, resultat);
            }
        }

        chemin.remove(chemin.size() - 1);      // backtrack
    }

    public static void main(String[] args) {
        List<Tuple> tuples = new ArrayList<>();
        tuples.add(new Tuple("A", "B"));
        tuples.add(new Tuple("A", "C"));
        tuples.add(new Tuple("B", "D"));
        tuples.add(new Tuple("D", "E"));
        tuples.add(new Tuple("F", "G"));
        tuples.add(new Tuple("H", null));      // parent sans enfant

        System.out.println(Arrays.deepToString(construire(tuples)));
    }
}