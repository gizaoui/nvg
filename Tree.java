import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Selection {

    public static void main(String[] args) {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();

        List<List<UUID>> t1 = List.of(
            List.of(a, b, c),
            List.of(b, c),
            List.of(c),
            List.of(d, b, c));
        List<UUID> t2 = List.of(b, c);

        Set<UUID> autorises = new HashSet<>(t2);

        List<UUID> resultat = t1.stream()
            .filter(l -> !l.isEmpty() && autorises.contains(l.get(0)))
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());

        System.out.println(resultat); // [UUID de b, UUID de c]
    }
}