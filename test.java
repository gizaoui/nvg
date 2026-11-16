import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

static <T, K> List<T> fusionner(List<T> a, List<T> b, Function<T, K> cle) {
    return Stream.concat(a.stream(), b.stream())
            .collect(Collectors.toMap(
                    cle,
                    Function.identity(),
                    (premier, second) -> premier,   // en cas de doublon, on garde le premier
                    LinkedHashMap::new))            // conserve l'ordre d'insertion
            .values()
            .stream()
            .toList();
}



List<Personne> fusion = fusionner(liste1, liste2, Personne::getId);
// ou avec une clé composite :
List<Personne> fusion2 = fusionner(liste1, liste2, p -> p.getNom() + "|" + p.getPrenom());