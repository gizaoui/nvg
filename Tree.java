import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Selection {

    public static void main(String[] args) {
        List<List<String>> t1 = List.of(
            List.of("A", "B", "C"),
            List.of("B", "C"),
            List.of("C"),
            List.of("D", "B", "C"));
        List<String> t2 = List.of("B", "C");

        Set<String> autorises = new HashSet<>(t2);

        List<List<String>> resultat = t1.stream()
            .filter(l -> !l.isEmpty() && autorises.contains(l.get(0)))
            .collect(Collectors.toList());

        System.out.println(resultat); // [[B, C], [C]]
    }
}