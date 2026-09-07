public static String solve(int width, int height, int length, int mass) {
    long volume = (long) width * height * length;
    
    boolean isBulky = volume >= 1000000 || width >= 150 || height >= 150 || length >= 150;
    boolean isHeavy = mass >= 20;

    if (isBulky && isHeavy) {
        return "REJECTED";
    }
    if (isBulky || isHeavy) {
        return "SPECIAL";
    }
    return "STANDARD";
}
