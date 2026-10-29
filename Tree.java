UUID[] t1 = {
        new UUID(0L, 1L),
        new UUID(0L, 2L),
        new UUID(0L, 3L),
        new UUID(0L, 2L)
};

Map<UUID, String> t2 = Map.of(
        new UUID(0L, 1L), "A",
        new UUID(0L, 2L), "B",
        new UUID(0L, 3L), "C");

// Un tableau de chaînes (une par UUID)
String[] t3 = Arrays.stream(t1)
        .map(t2::get)
        .toArray(String[]::new);
// [A, B, C, B]

// Une seule chaîne
String chaine = Arrays.stream(t1)
        .map(t2::get)
        .collect(Collectors.joining());
// "ABCB"

System.out.println(Arrays.toString(t3));
System.out.println(chaine);