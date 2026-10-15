package com.buracoarcano;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Cada fase × jogador × origem de compra × ação válida ou inválida. */
public final class ActionsMatrixTest {
    private static GameEngine prepared(long seed) {
        return QaSupport.game("normal", seed);
    }
    private static GameEngine.Card move(GameEngine game, int suit, int rank) {
        for (GameEngine.Card c : game.hands[0]) if (c.suit == suit && c.rank == rank) return c;
        for (int i = 0; i < game.stock.size(); i++) {
            GameEngine.Card c = game.stock.get(i);
            if (c.suit == suit && c.rank == rank) {
                // Troca uma carta da mão por uma do monte, conservando exatamente 104.
                int swap = -1;
                for (int j = 0; j < game.hands[0].size(); j++) {
                    GameEngine.Card old = game.hands[0].get(j);
                    if (old.suit != suit || old.rank < 4 || old.rank > 9) {
                        swap = j; break;
                    }
                }
                if (swap == -1) swap = 0;
                game.stock.set(i, game.hands[0].remove(swap));
                game.hands[0].add(c);
                return c;
            }
        }
        throw new AssertionError("Não encontrada no monte nem na mão: " + suit + "/" + rank);
    }
    public static void main(String[] args) {
        long start = System.nanoTime(), before = QaSupport.assertions;
        for (int difficultyIndex = 0; difficultyIndex < 4; difficultyIndex++) {
            String difficulty = new String[]{"facil", "normal", "dificil", "lendario"}[difficultyIndex];
            for (int origin = 0; origin <= 1; origin++)
                for (int player = 0; player < 4; player++) {
                    GameEngine g = QaSupport.game(difficulty, difficultyIndex * 200 + origin * 10 + player);
                    QaSupport.ok(!g.discard(player, g.hands[player].get(0).id), "descartar antes de comprar");
                    QaSupport.ok(!g.meld(player, "0-0-3", -1), "baixar antes de comprar");
                    if (player != 0) {
                        QaSupport.ok(!g.buyStock(player), "jogador errado não pode comprar");
                        QaSupport.ok(!g.buyDiscard(player), "jogador errado não pode recolher lixo");
                    } else {
                        int oldHand = g.hands[0].size(), oldDiscard = g.discardPile.size();
                        QaSupport.ok(origin == 0 ? g.buyStock(0) : g.buyDiscard(0), "compra válida");
                        QaSupport.ok(g.hands[0].size() == oldHand + (origin == 0 ? 1 : oldDiscard), "mão aumentou na compra");
                        QaSupport.ok(!g.buyStock(0) && !g.buyDiscard(0), "não pode comprar duas vezes");
                        QaSupport.ok(!g.meld(0, "carta-inexistente", -1), "não pode baixar carta inexistente");
                        QaSupport.ok(!g.meld(0, g.hands[0].get(0).id + "," + g.hands[0].get(0).id, -1), "não pode repetir seleção");
                        QaSupport.ok(!g.meld(0, g.hands[0].get(0).id, 99), "grupo inexistente");
                        QaSupport.ok(!g.discard(0, "carta-inexistente"), "não pode descartar inexistente");
                        QaSupport.ok(g.discard(0, g.hands[0].get(0).id), "descarte válido");
                        QaSupport.ok(!g.discard(0, g.hands[0].get(0).id), "não pode descartar duas vezes");
                    }
                    QaSupport.integrity(g, difficulty + "/" + origin + "/" + player);
                }
        }
        QaSupport.report("fase, turnos, 4 IA, duas compras", start, before);
        start = System.nanoTime(); before = QaSupport.assertions;
        for (int suit = 0; suit < 4; suit++) {
            GameEngine g = prepared(221 + suit);
            QaSupport.ok(g.buyStock(0), "comprar");
            List<GameEngine.Card> run = Arrays.asList(move(g, suit, 4), move(g, suit, 5), move(g, suit, 6));
            QaSupport.ok(g.meld(0, QaSupport.ids(run), -1), "baixar sequência válida");
            QaSupport.ok(g.melds[0].size() == 1, "criou jogo 1");
            QaSupport.ok(!g.meld(0, QaSupport.ids(run), -1), "não pode baixar cartas que não estão na mão");
            GameEngine.Card extra = move(g, suit, 7);
            QaSupport.ok(g.meld(0, extra.id, 0), "ampliar jogo próprio");
            QaSupport.ok(g.melds[0].get(0).cards.size() == 4, "jogo expandido");
            QaSupport.ok(!g.meld(0, g.hands[0].get(0).id, 1), "proibido índice inexistente");
            QaSupport.integrity(g, "jogo+extensão suit=" + suit);
        }
        QaSupport.report("baixar e ampliar jogos por naipe", start, before);
        start = System.nanoTime(); before = QaSupport.assertions;
        for (int suit = 0; suit < 4; suit++) {
            GameEngine g = prepared(443 + suit);
            QaSupport.ok(g.buyStock(0), "compra antes do morto");
            List<GameEngine.Card> run = Arrays.asList(move(g, suit, 7), move(g, suit, 8), move(g, suit, 9));
            for (GameEngine.Card c : new ArrayList<>(g.hands[0])) {
                if (!run.contains(c)) {
                    g.hands[0].remove(c);
                    g.stock.add(c);
                }
            }
            QaSupport.ok(g.hands[0].size() == 3, "preparou 3 cartas");
            QaSupport.ok(g.meld(0, QaSupport.ids(run), -1), "baixou as 3");
            QaSupport.ok(g.deadTaken[0] && g.hands[0].size() == 11, "morto automático por baixar toda a mão");
            QaSupport.integrity(g, "morto por baixa");
        }
        QaSupport.report("retirada do morto por baixa", start, before);
    }
}
