public class MaxSubarrayFixedSize {

    public static int[] maxSubarrayIndicesFixedSize(int[] nums, int k) {
        if (nums == null || nums.length < k || k <= 0) {
            return new int[]{};
        }

        // Calcul de la somme de la première fenêtre
        int currentSum = 0;
        for (int i = 0; i < k; i++) {
            currentSum += nums[i];
        }

        int maxSum = currentSum;
        int bestStart = 0;

        // Glissement de la fenêtre sur le reste du tableau
        for (int i = k; i < nums.length; i++) {
            currentSum += nums[i] - nums[i - k];
            if (currentSum > maxSum) {
                maxSum = currentSum;
                bestStart = i - k + 1;
            }
        }

        return new int[]{bestStart, bestStart + k - 1};
    }

    public static void main(String[] args) {
        int[] tableau = {10, 20, 30};
        int k = 2; // Taille de la sous-suite recherchée

        int[] indices = maxSubarrayIndicesFixedSize(tableau, k);
        System.out.println("Indices pour k=" + k + " : [" + indices[0] + ", " + indices[1] + "]");
    }
}
