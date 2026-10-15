package com.buracoarcano;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Infraestrutura compartilhada, sem dependências de JUnit ou de JS. */
public final class QaSupport {
    private QaSupport() {}
    public static long assertions = 0;
    public static void ok(boolean condition, String explanation) {
        assertions++;
        if (!condition) throw new AssertionError(explanation);
    }
    public static GameEngine game(String difficulty, long seed) {
        Random random = new Random(seed);
        return new GameEngine(difficulty, random::nextInt);
    }
    public static GameEngine.Card card(int copy, int suit, int rank) {
        return new GameEngine.Card(copy, suit, rank);
    }
    public static void integrity(GameEngine game, String context) {
        ok(game.totalCards() == 104, context + ": contagem != 104, era " + game.totalCards());
        Set<String> ids = new HashSet<>();
        for (GameEngine.Card c : game.stock) ok(ids.add(c.id), context + ": carta duplicada no monte " + c.id);
        for (GameEngine.Card c : game.discardPile) ok(ids.add(c.id), context + ": duplicada no lixo " + c.id);
        for (List<GameEngine.Card> cards : game.hands)
            for (GameEngine.Card c : cards) ok(ids.add(c.id), context + ": duplicada na mão " + c.id);
        for (List<GameEngine.Card> cards : game.deads)
            if (cards != null) for (GameEngine.Card c : cards) ok(ids.add(c.id), context + ": duplicada no morto " + c.id);
        for (List<GameEngine.Meld> melds : game.melds)
            for (GameEngine.Meld m : melds) for (GameEngine.Card c : m.cards)
                ok(ids.add(c.id), context + ": duplicada na mesa " + c.id);
        ok(ids.size() == 104, context + ": conjunto incompleto");
    }
    public static String ids(List<GameEngine.Card> cards) {
        StringBuilder result = new StringBuilder();
        for (GameEngine.Card c : cards) {
            if (result.length() > 0) result.append(',');
            result.append(c.id);
        }
        return result.toString();
    }
    public static void report(String title, long start, long previous) {
        System.out.printf("PASS | %-34s | %10d verificações | %6d ms%n",
            title, assertions - previous, (System.nanoTime() - start) / 1_000_000L);
    }
}
