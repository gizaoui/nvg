import java.util.*;
import java.util.stream.Collectors;

class Tuple {
    String parent;
    String enfant;

    Tuple(String parent, String enfant) {
        this.parent = parent;
        this.enfant = enfant;
    }
}

public class ArbreTableau {

    static String[][] construire(List<Tuple> tuples) {
        // parent -> liste de ses enfants (ordre d'insertion conservé)
        Map<String, List<String>> index = tuples.stream()
            .collect(Collectors.groupingBy(
                t -> t.parent,
                LinkedHashMap::new,
                Collectors.mapping(t -> t.enfant, Collectors.toList())));

        // tous les noeuds qui sont enfants de quelqu'un
        Set<String> enfants = tuples.stream()
            .map(t -> t.enfant)
            .collect(Collectors.toSet());

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
        if (fils == null) {                    // feuille : on enregistre le chemin
            resultat.add(new ArrayList<>(chemin));
        } else {
            for (String f : fils) {
                parcourir(f, index, chemin, resultat);
            }
        }

        chemin.remove(chemin.size() - 1);      // backtrack
    }

    public static void main(String[] args) {
        List<Tuple> tuples = List.of(
            new Tuple("A", "B"),
            new Tuple("A", "C"),
            new Tuple("B", "D"),
            new Tuple("D", "E"),
            new Tuple("F", "G"));

        String[][] resultat = construire(tuples);
        System.out.println(Arrays.deepToString(resultat));
    }
}