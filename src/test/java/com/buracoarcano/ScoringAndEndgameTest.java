package com.buracoarcano;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Matrizes de pontuação por carta, canastra, batida e reinício de rodada. */
public final class ScoringAndEndgameTest {
    private static GameEngine.Meld meld(int suit, int from, int size, boolean dirty) {
        List<GameEngine.Card> cards = new ArrayList<>();
        for (int k = 0; k < size; k++) {
            int rank = from + k;
            cards.add(QaSupport.card(rank == 14 ? 1 : 0, suit, rank == 14 ? 1 : rank));
        }
        if (dirty) {
            for (int k = cards.size() - 1; k >= 0; k--) {
                if (cards.get(k).rank != 2) {
                    cards.set(k, QaSupport.card(1, (suit + 1) % 4, 2));
                    break;
                }
            }
        }
        return new GameEngine.Meld(cards);
    }
    public static void main(String[] args) {
        long start = System.nanoTime(), before = QaSupport.assertions;
        for (int s = 0; s < 4; s++) {
            for (int rank = 1; rank <= 13; rank++) {
                int expected = rank == 1 ? 15 : rank == 2 || rank >= 8 ? 10 : 5;
                QaSupport.ok(GameEngine.value(QaSupport.card(0, s, rank)) == expected, "pontuação de " + rank);
            }
            for (int length = 3; length <= 14; length++) {
                GameEngine.Meld clean = meld(s, 1, length, false);
                QaSupport.ok(GameEngine.validate(clean.cards) != null, "canastra limpa consecutiva " + length);
                int bonus = length < 7 ? 0 : length < 13 ? 200 : length == 13 ? 500 : 1000;
                QaSupport.ok(GameEngine.meldBonus(clean) == bonus, "bônus limpo " + length);
                GameEngine.Meld dirty = meld(s, 1, length, true);
                if (GameEngine.validate(dirty.cards) != null)
                    QaSupport.ok(GameEngine.meldBonus(dirty) == (length < 7 ? 0 : 100),
                        "bônus sujo " + length);
            }
        }
        QaSupport.report("4 naipes x valores e canastras 3-14", start, before);
        start = System.nanoTime(); before = QaSupport.assertions;
        GameEngine g = QaSupport.game("normal", 20261015);
        QaSupport.ok(g.round == 1, "primeira rodada");
        QaSupport.ok(g.totals[0] == 0 && g.totals[1] == 0, "placar inicial");
        g.totals[0] = 735; g.totals[1] = 590;
        g.newRound();
        QaSupport.ok(g.round == 2, "avançou rodada");
        QaSupport.ok(g.totals[0] == 735 && g.totals[1] == 590, "placar preservado");
        QaSupport.ok(g.lastRoundScore == null, "resultado anterior zerado");
        QaSupport.ok(!g.deadTaken[0] && !g.deadTaken[1], "morto reiniciado");
        QaSupport.integrity(g, "reinício de rodada");
        QaSupport.report("placar acumulado / nova rodada", start, before);
    }
}
