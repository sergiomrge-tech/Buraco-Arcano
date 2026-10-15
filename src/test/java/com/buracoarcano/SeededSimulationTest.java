package com.buracoarcano;

import java.util.ArrayList;
import java.util.List;

/**
 * Testes estocásticos REPRODUZÍVEIS: toda falha identifica dificuldade,
 * seed e passo para reproduzir. Usuário humano compra e descarta; demais
 * posições executam bot real. Integridade verificada depois de cada ação.
 *
 * Argumentos: [sementes por dificuldade] [passos máximos por partida].
 */
public final class SeededSimulationTest {
    private static final String[] MODES = {"facil", "normal", "dificil", "lendario"};
    public static void main(String[] args) {
        int samples = args.length > 0 ? Integer.parseInt(args[0]) : 30;
        int maxSteps = args.length > 1 ? Integer.parseInt(args[1]) : 180;
        long start = System.nanoTime(), before = QaSupport.assertions;
        int games = 0, steps = 0, finished = 0;
        for (String level : MODES) for (int sample = 0; sample < samples; sample++) {
            long seed = 20261015000L + 7919L * sample + 411L * games;
            GameEngine g = QaSupport.game(level, seed);
            QaSupport.integrity(g, "início " + level + ":" + seed);
            for (int step = 0; step < maxSteps && !g.stockEnded; step++) {
                int previousPlayer = g.player;
                String previousPhase = g.phase;
                try {
                    if (g.player == 0) {
                        boolean pile = sample % 3 == 0 && step % 4 == 0 && !g.discardPile.isEmpty();
                        boolean bought = pile ? g.buyDiscard(0) : g.buyStock(0);
                        if (!bought) {
                            QaSupport.ok(g.stockEnded, "compra fracassou sem terminar rodada");
                            break;
                        }
                        if (!g.hands[0].isEmpty()) {
                            boolean discarded = false;
                            for (GameEngine.Card c : new ArrayList<>(g.hands[0])) {
                                if (g.discard(0, c.id)) { discarded = true; break; }
                            }
                            QaSupport.ok(discarded || g.stockEnded,
                                "jogador sem ação legal, fase=" + g.phase);
                        }
                    } else g.aiPlay(g.player);
                    QaSupport.integrity(g, level + " seed=" + seed + " step=" + step);
                    QaSupport.ok(g.stockEnded || g.player != previousPlayer || !g.phase.equals(previousPhase),
                        "turno travado seed=" + seed + " step=" + step + " dificuldade=" + level +
                            " hand=" + g.hands[g.player].size() + " deadTaken=" + g.deadTaken[g.player%2] +
                            " deadPiles=" + (g.deads[0]==null?0:g.deads[0].size()) + "/" +
                            (g.deads[1]==null?0:g.deads[1].size()) + " stock=" + g.stock.size() +
                            " last=" + g.message);
                    steps++;
                } catch (Throwable failure) {
                    throw new AssertionError("SEED REPRODUZÍVEL: level=" + level + " seed=" + seed +
                        " step=" + step + " player=" + previousPlayer +
                        " phase=" + previousPhase + " | " + failure, failure);
                }
            }
            if (g.stockEnded) {
                QaSupport.ok(g.lastRoundScore != null, "fim precisa guardar pontos");
                finished++;
            }
            games++;
        }
        QaSupport.report("partidas em 4 dificuldades", start, before);
        System.out.printf("RESULTADO SIMULAÇÃO: partidas=%d, passos=%d, encerradas=%d, sementes/modo=%d%n",
            games, steps, finished, samples);
    }
}
