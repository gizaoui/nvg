@SafeVarargs
static List<Element> concatSansDoublons(List<Element>... listes) {
    return Arrays.stream(listes)
            .flatMap(l -> l.stream())
            .distinct()
            .toList();
}