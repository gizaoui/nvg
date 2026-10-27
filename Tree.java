List<List<String>> listes = List.of(
        List.of("A", "B", "C"),
        List.of("D", "B", "C"));

List<List<String>> sansDoublons = listes.stream()
        .flatMap(ligne -> IntStream.range(0, ligne.size())
                .mapToObj(i -> ligne.subList(i, ligne.size())))
        .distinct()
        .toList();
// [[A, B, C], [B, C], [C], [D, B, C]]