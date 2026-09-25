// Soit un tableau d'entiers représentant les bénéfices par mois.
// On souhaite connaître la plage de mois consécutif enregistrant le plus grand bénéfice.
// go run MaxProfit.go 
// go build -o MaxProfit MaxProfit.go && ./MaxProfit

package main

import "fmt"

type MaxProfit struct{}

func (m MaxProfit) MaxProfitIndices(nums []int) []int {
	if len(nums) == 0 {
		return []int{}
	}

	maxSoFar := nums[0]
	maxEndingHere := nums[0]

	start := 0
	bestStart := 0
	bestEnd := 0

	for i := 1; i < len(nums); i++ {
		if nums[i] > maxEndingHere+nums[i] {
			maxEndingHere = nums[i]
			start = i
		} else {
			maxEndingHere += nums[i]
		}

		if maxEndingHere > maxSoFar {
			maxSoFar = maxEndingHere
			bestStart = start
			bestEnd = i
		}
	}

	return []int{bestStart, bestEnd}
}

// Résultat : Indices pour k=2 : [4,5] -> 3+8=11
func main() {
	tableau := []int{1,2,3,4,3,8,3,5,1} // Liste des bénéfices de janvier à septembre.
	
	ms := MaxProfit{}
	indices := ms.MaxProfitIndices(tableau)

	if len(indices) == 2 { // 2 : Taille de la sous-suite recherchée
		fmt.Printf("Indices : [%d, %d]\n", indices[0], indices[1])
	}
}
