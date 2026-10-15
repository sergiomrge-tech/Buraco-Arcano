package com.buracoarcano;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * ENUMERAÇÃO EXAUSTIVA: C(104,3) = 182.104 trios de cartas físicas.
 * Para comprimentos maiores: casos válidos construídos e sorteios determinísticos.
 * O oráculo calcula atribuições de posições independentemente de GameEngine.validate.
 */
public final class ExhaustiveSequencesTest {
    private static final List<GameEngine.Card> DECK = new ArrayList<>();
    static {
        for (int copy = 0; copy < 2; copy++)
            for (int suit = 0; suit < 4; suit++)
                for (int rank = 1; rank <= 13; rank++)
                    DECK.add(QaSupport.card(copy, suit, rank));
    }
    private static boolean oracle(List<GameEngine.Card> cards) {
        int length = cards.size();
        if (length < 3 || length > 14) return false;
        int suit = -1, normals = 0;
        for (GameEngine.Card c : cards) {
            if (c.rank == 2) continue;
            normals++;
            if (suit == -1) suit = c.suit;
            else if (suit != c.suit) return false;
        }
        if (normals == 0) return false;
        for (int start = 1; start + length - 1 <= 14; start++)
            if (assign(cards, 0, suit, start, length, 0, 0)) return true;
        return false;
    }
    private static boolean assign(List<GameEngine.Card> cards, int index, int suit,
                                  int start, int length, int occupied, int wild) {
        if (index == cards.size()) return true;
        GameEngine.Card card = cards.get(index);
        if (card.rank == 2) {
            int natural = 2 - start;
            if (card.suit == suit && natural >= 0 && natural < length &&
                (occupied & (1 << natural)) == 0 &&
                assign(cards, index + 1, suit, start, length, occupied | 1 << natural, wild)) return true;
            if (wild == 0) {
                for (int slot = 0; slot < length; slot++)
                    if ((occupied & (1 << slot)) == 0 &&
                        assign(cards, index + 1, suit, start, length, occupied | 1 << slot, 1)) return true;
            }
            return false;
        }
        if (card.suit != suit) return false;
        int low = card.rank - start;
        if (low >= 0 && low < length && (occupied & (1 << low)) == 0 &&
            assign(cards, index + 1, suit, start, length, occupied | 1 << low, wild)) return true;
        if (card.rank == 1) {
            int high = 14 - start;
            if (high >= 0 && high < length && (occupied & (1 << high)) == 0 &&
                assign(cards, index + 1, suit, start, length, occupied | 1 << high, wild)) return true;
        }
        return false;
    }
    private static void compare(List<GameEngine.Card> cards, String label) {
        boolean expected = oracle(cards);
        boolean actual = GameEngine.validate(cards) != null;
        QaSupport.ok(expected == actual,
            label + ": esperado=" + expected + " obtido=" + actual + " cartas=" + QaSupport.ids(cards));
    }
    private static List<GameEngine.Card> span(int suit, int first, int length) {
        List<GameEngine.Card> result = new ArrayList<>();
        for (int i = first; i < first + length; i++) {
            int rank = i == 14 ? 1 : i;
            // No 14-cartas, dois Ases usam cópias físicas diferentes.
            int copy = i == 14 ? 1 : 0;
            result.add(QaSupport.card(copy, suit, rank));
        }
        return result;
    }
    public static void main(String[] args) {
        long start = System.nanoTime(), before = QaSupport.assertions;
        for (int a = 0; a < DECK.size() - 2; a++)
            for (int b = a + 1; b < DECK.size() - 1; b++)
                for (int c = b + 1; c < DECK.size(); c++)
                    compare(Arrays.asList(DECK.get(a), DECK.get(b), DECK.get(c)), "trio " + a + "/" + b + "/" + c);
        QaSupport.report("182.104 trios físicos (exaustivo)", start, before);
        start = System.nanoTime(); before = QaSupport.assertions;
        for (int suit = 0; suit < 4; suit++)
            for (int length = 3; length <= 14; length++)
                for (int first = 1; first <= 15 - length; first++) {
                    List<GameEngine.Card> pure = span(suit, first, length);
                    compare(pure, "sequência completa");
                    QaSupport.ok(GameEngine.validate(pure) != null, "Sequência limpa válida " + QaSupport.ids(pure));
                    QaSupport.ok(GameEngine.validate(pure).clean, "Sequência sem curingas tem de ser limpa");
                    for (int remove = 0; remove < pure.size(); remove++) {
                        List<GameEngine.Card> wild = new ArrayList<>(pure);
                        if (wild.get(remove).rank == 2) continue;
                        wild.set(remove, QaSupport.card(1, (suit + 1) % 4, 2));
                        compare(wild, "substituição por 2 de outro naipe");
                    }
                }
        QaSupport.report("sequências A baixo/A alto e 2", start, before);
        start = System.nanoTime(); before = QaSupport.assertions;
        Random random = new Random(9992833L);
        List<GameEngine.Card> sample = new ArrayList<>(DECK);
        for (int i = 0; i < 15_000; i++) {
            Collections.shuffle(sample, random);
            int n = 4 + (i % 11);
            compare(sample.subList(0, n), "amostra reprodutível " + i);
        }
        QaSupport.report("15.000 conjuntos 4 a 14 cartas", start, before);
    }
}
