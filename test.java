public class MaximumSubarray {

    public static int[] maxSubarrayIndices(int[] nums) {
        if (nums == null || nums.length == 0) {
            return new int[]{};
        }

        int maxSoFar = nums[0];
        int maxEndingHere = nums[0];

        int start = 0;
        int bestStart = 0;
        int bestEnd = 0;

        for (int i = 1; i < nums.length; i++) {
            if (nums[i] > maxEndingHere + nums[i]) {
                maxEndingHere = nums[i];
                start = i;
            } else {
                maxEndingHere += nums[i];
            }

            if (maxEndingHere > maxSoFar) {
                maxSoFar = maxEndingHere;
                bestStart = start;
                bestEnd = i;
            }
        }

        return new int[]{bestStart, bestEnd};
    }

    public static void main(String[] args) {
        int[] tableau = {10, 20, 30};
        int[] indices = maxSubarrayIndices(tableau);

        System.out.println("Indices : [" + indices[0] + ", " + indices[1] + "]");
    }
}
