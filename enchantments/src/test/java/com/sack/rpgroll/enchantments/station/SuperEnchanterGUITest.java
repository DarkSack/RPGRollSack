package com.sack.rpgroll.enchantments.station;

import com.sack.rpgroll.enchantments.station.SuperEnchanterGUI.Cell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuperEnchanterGUITest {

    @Test
    void cellsShowWhatStaysWhatIsAddedAndWhatGoes() {
        // tiene III, quiere V, tope VI
        assertEquals(Cell.HAVE, SuperEnchanterGUI.cell(3, 3, 5, 6, false));
        assertEquals(Cell.ADD, SuperEnchanterGUI.cell(4, 3, 5, 6, false));
        assertEquals(Cell.FREE, SuperEnchanterGUI.cell(6, 3, 5, 6, false));
        assertEquals(Cell.NONE, SuperEnchanterGUI.cell(7, 3, 5, 6, false));
        // tiene III, quiere I
        assertEquals(Cell.HAVE, SuperEnchanterGUI.cell(1, 3, 1, 6, false));
        assertEquals(Cell.REMOVE, SuperEnchanterGUI.cell(2, 3, 1, 6, false));
        // bloqueado por un conflicto
        assertEquals(Cell.BLOCKED, SuperEnchanterGUI.cell(1, 0, 0, 3, true));
    }

    @Test
    void clickingTheChosenLevelAgainStepsDown() {
        assertEquals(4, SuperEnchanterGUI.clicked(4, 2));
        assertEquals(3, SuperEnchanterGUI.clicked(4, 4));
        assertEquals(0, SuperEnchanterGUI.clicked(1, 1));
    }
}
