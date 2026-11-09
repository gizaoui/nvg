import java.util.*;

public class FusionUuid {

    @SafeVarargs
    public static <T> List<T> fusionnerSansDoublons(List<? extends T>... listes) {
        Set<T> resultat = new LinkedHashSet<>();
        for (List<? extends T> liste : listes) {
            resultat.addAll(liste);
        }
        return new ArrayList<>(resultat);
    }

    public static void main(String[] args) {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        UUID e = UUID.randomUUID();

        List<UUID> l1 = List.of(a, b, c);
        List<UUID> l2 = List.of(d, c, e); // c apparaît dans les deux listes

        List<UUID> resultat = fusionnerSansDoublons(l1, l2);

        resultat.forEach(System.out::println);
        System.out.println(resultat.size()); // 5
        System.out.println(resultat.equals(List.of(a, b, c, d, e))); // true
    }
}