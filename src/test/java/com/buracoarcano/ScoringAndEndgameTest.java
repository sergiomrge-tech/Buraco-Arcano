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

        start = System.nanoTime(); before = QaSupport.assertions;
        // Reproduz deadlock de seed 20261052320: um morto foi absorvido
        // pelo monte e um jogador ficou com zero cartas, sem ter pego o morto.
        GameEngine noDead = QaSupport.game("normal", 20261052320L);
        QaSupport.ok(noDead.buyStock(0), "compra de preparação");
        for (int i = 0; i < 2; i++) {
            noDead.stock.addAll(noDead.deads[i]);
            noDead.deads[i] = null;
        }
        while (noDead.hands[0].size() > 1)
            noDead.stock.add(noDead.hands[0].remove(0));
        QaSupport.ok(noDead.discard(0, noDead.hands[0].get(0).id),
            "último descarte sem morto não pode travar");
        QaSupport.ok(noDead.stockEnded && "end".equals(noDead.phase),
            "rodada encerra ao ficar sem cartas e sem mortos disponíveis");
        QaSupport.ok(noDead.winner == null, "encerramento sem bônus de batida");
        QaSupport.ok(noDead.lastRoundScore != null, "pontos do encerramento existem");
        QaSupport.integrity(noDead, "fim sem morto");
        QaSupport.report("fim com morto indisponível", start, before);

    }
}
