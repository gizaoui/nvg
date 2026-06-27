package pack;

import org.junit.Test;
import static org.junit.Assert.*;

public class CalcTest {
    @Test
    public void canConstructAPersonWithAName() {
        Calc calc = new Calc();
        assertEquals(7, calc.add(4, 3));
    }
}
