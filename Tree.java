List<List<UUID>> listResultUUID = data.stream()
        .flatMap(e -> IntStream.range(0, e.size())
                .mapToObj(i -> e.subList(i, e.size())))
        .distinct()
        .toList();