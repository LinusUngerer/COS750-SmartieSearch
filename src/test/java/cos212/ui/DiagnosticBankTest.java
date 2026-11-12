package cos212.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DiagnosticBankTest {

    @Test
    void everyQuestionIsTaggedWithItsTreeAndTextRepresentations() {
        for (DiagnosticBank.Item item : DiagnosticBank.ITEMS) {
            assertTrue(item.modes().contains(RepresentationMode.VISUAL), item.question());
            assertTrue(item.modes().contains(RepresentationMode.READ_WRITE), item.question());
        }
    }
}
