List<List<UUID>> sansDoublons = Arrays.stream(tableau)
        .flatMap(ligne -> IntStream.range(0, ligne.length)
                .mapToObj(i -> List.of(Arrays.copyOfRange(ligne, i, ligne.length))))
        .distinct()
        .toList();