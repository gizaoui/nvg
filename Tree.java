List<List<String>> sansDoublons = Arrays.stream(tableau)
        .flatMap(ligne -> IntStream.range(0, ligne.length)
                .mapToObj(i -> Arrays.copyOfRange(ligne, i, ligne.length)))
        .map(Arrays::asList)
        .distinct()
        .toList();




String[][] resultat = sansDoublons.stream()
        .map(l -> l.toArray(String[]::new))
        .toArray(String[][]::new);