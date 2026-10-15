package com.buracoarcano;

import java.util.Random;

/** Executar: javac ... && java -cp out com.buracoarcano.GameSmokeTest */
public final class GameSmokeTest {
    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError(why);
    }
    private static int firstCardId(Game g) {
        String snap = g.snapshot();
        int idx = snap.indexOf("\"hands\":[");
        idx = snap.indexOf("\"id\":", idx);
        int begin = idx + 5, end = begin;
        while (Character.isDigit(snap.charAt(end))) end++;
        return Integer.parseInt(snap.substring(begin, end));
    }
    public static void main(String[] args) {
        for (int seed = 0; seed < 250; seed++) {
            Game g = new Game("normal", new Random(seed)::nextInt);
            check(g.cardTotal() == 104, "contagem inicial");
            for (int p = 0; p < 4; p++) check(g.handSize(p) == 11, "11 por mão");
            check(g.snapshot().contains("\"stock\":37"), "monte de 37");
            g.draw(0, false);
            check(g.cardTotal() == 104 && g.handSize(0) == 12, "compra");
            g.discard(0, firstCardId(g));
            check(g.currentPlayer() == 1 && g.cardTotal() == 104, "passagem de turno");
            for (int round = 0; round < 10 && g.winner() == -1; round++) {
                while (g.currentPlayer() != 0 && g.winner() == -1) {
                    g.botStep();
                    check(g.cardTotal() == 104, "bot não pode perder cartas");
                }
                if (g.winner() == -1) {
                    g.draw(0, false);
                    g.discard(0, firstCardId(g));
                    check(g.cardTotal() == 104, "jogador não pode perder cartas");
                }
            }
        }
        Game a = new Game("facil", new Random(39)::nextInt);
        Game b = new Game("facil", new Random(39)::nextInt);
        Game c = new Game("facil", new Random(40)::nextInt);
        check(a.snapshot().equals(b.snapshot()), "sementes iguais produzem shuffle igual");
        check(!a.snapshot().equals(c.snapshot()), "sementes diferentes alteram distribuição");
        System.out.println("OK: 250 partidas simuladas, baralho, mortos, turnos, bots e contagem.");
    }
}
