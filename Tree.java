List<Map<UUID, List<UUID>>> allTasks = new ArrayList<>();

allTasks.add(Map.of(t01, List.of(t02, t04)));
allTasks.add(Map.of(t02, List.of(t03)));
allTasks.add(Map.of(t04, List.of()));