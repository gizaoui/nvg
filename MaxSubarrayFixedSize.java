// Soit un tableau d'entiers représentant les bénéfices par mois.
// On souhaite connaître la plage de mois consécutif enregistrant le plus grand bénéfice.


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

    // Résultat : Indices pour k=2 : [4,5] -> 3+8=11
    public static void main(String[] args) {

        int[] tableau = {1,2,3,4,3,8,3,5,1}; // Liste des bénéfices de janvier à septembre.
        int k = 2; // Taille de la sous-suite recherchée

        int[] indices = maxSubarrayIndicesFixedSize(tableau, k);
        System.out.println("Indices pour k=" + k + " : [" + indices[0] + ", " + indices[1] + "]");
    }
}
