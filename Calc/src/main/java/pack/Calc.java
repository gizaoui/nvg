package pack;

// pack.CalcTest

public class Calc {
	
	public Calc() {
		System.out.println("MyTest");
	}

	public int add(int a, int b) {
		return a + b;
	}
	
	public static void main(String[] args) {
		Calc t = new Calc();
		System.out.println("Résultat : 3 + 4 = " + t.add(3, 4));

		"Hello, Baeldung!".chars().forEach(c -> { 
		    System.out.print((char) c);
		});
	}
}