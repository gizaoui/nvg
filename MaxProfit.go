package main

// Soit un tableau d'entiers représentant les bénéfices par mois.
// On souhaite connaître la plage de mois consécutif enregistrant le plus grand bénéfice.
// go run MaxProfit.go 
// go build -o MaxProfit MaxProfit.go && ./MaxProfit


import "fmt"


func MaxProfit(nums []int, k int) []int {
	if nums == nil || len(nums) < k || k <= 0 {
		return []int{}
	}

	// Calcul de la somme de la première fenêtre
	currentSum := 0
	for i := 0; i < k; i++ {
		currentSum += nums[i]
	}

	maxSum := currentSum
	bestStart := 0

	// Glissement de la fenêtre sur le reste du tableau
	for i := k; i < len(nums); i++ {
		currentSum += nums[i] - nums[i-k]
		if currentSum > maxSum {
			maxSum = currentSum
			bestStart = i - k + 1
		}
	}

	return []int{bestStart, bestStart + k - 1}
}

// Résultat : Indices pour k=2 : [4,5] -> 3+8=11
func main() {
	tableau := []int{1, 2, 3, 4, 3, 8, 3, 5, 1} // Liste des bénéfices de janvier à septembre.
	k := 2                                       // Taille de la sous-suite recherchée

	indices := MaxProfit(tableau, k)

	if len(indices) == 2 {
		fmt.Printf("Indices pour k=%d : [%d, %d]\n", k, indices[0], indices[1])
	}
}
