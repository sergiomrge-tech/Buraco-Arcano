package com.buracoarcano;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Motor Java do Buraco Arcano, sem dependência de DOM.
 *
 * Escopo da prévia: Buraco Aberto 2v2, dois baralhos SEM curingas impressos,
 * quatro mãos de 11, dois mortos de 11, 2 como curinga, uma única sequência
 * por naipe/rank, pelo menos três cartas por jogo, até um curinga por jogo.
 * Para bater é preciso pegar o morto e ter uma canastra limpa.
 */
public final class Game {
    public interface RandomIndex { int next(int exclusiveBound); }

    public static final String[] SUITS = {"S", "H", "C", "D"};
    public static final String[] DIFFICULTIES = {"facil", "normal", "dificil", "especialista"};

    public static final class Card {
        public final int id, rank;
        public final String suit;
        Card(int id, int rank, String suit) { this.id = id; this.rank = rank; this.suit = suit; }
        public int points() { return rank == 1 ? 15 : rank == 2 ? 20 : rank >= 8 ? 10 : 5; }
        public boolean isWild() { return rank == 2; }
    }

    public static final class Meld {
        public boolean valid, clean, canasta;
        public int first, last, points;
        public String suit;
    }

    private final RandomIndex rng;
    private final List<Card>[] hands;
    private final List<Card>[] melds;
    private final List<Card> stock = new ArrayList<>();
    private final List<Card> discard = new ArrayList<>();
    private final List<Card>[] mortos;
    private final List<String> history = new ArrayList<>();
    private final boolean[] deadTaken = {false, false};

    private final String difficulty;
    private int currentPlayer = 0, turn = 1;
    private String phase = "draw", lastAction = "Nova partida";
    private int winner = -1;
    private final boolean[] justGotDead = {false, false};

    @SuppressWarnings("unchecked")
    public Game(String difficulty, RandomIndex rng) {
        this.rng = rng;
        if (!isDifficulty(difficulty)) throw new IllegalArgumentException("Dificuldade inválida");
        this.difficulty = difficulty;
        hands = (List<Card>[]) new List[4];
        melds = (List<Card>[]) new List[2];
        mortos = (List<Card>[]) new List[2];
        for (int i = 0; i < 4; i++) hands[i] = new ArrayList<>();
        for (int i = 0; i < 2; i++) { melds[i] = new ArrayList<>(); mortos[i] = new ArrayList<>(); }
        List<Card> cards = new ArrayList<>();
        for (int deck = 0; deck < 2; deck++)
            for (int suit = 0; suit < 4; suit++)
                for (int rank = 1; rank <= 13; rank++)
                    cards.add(new Card(deck * 52 + suit * 13 + rank, rank, SUITS[suit]));
        shuffle(cards);
        for (int i = 0; i < 11; i++) for (int p = 0; p < 4; p++) hands[p].add(take(cards));
        for (int i = 0; i < 11; i++) for (int t = 0; t < 2; t++) mortos[t].add(take(cards));
        discard.add(take(cards));
        stock.addAll(cards);
        note("Partida iniciada: 104 cartas, duas equipes e dois mortos.");
    }

    public static boolean isDifficulty(String value) {
        for (String d : DIFFICULTIES) if (d.equals(value)) return true;
        return false;
    }

    private Card take(List<Card> cards) { return cards.remove(cards.size() - 1); }
    private void shuffle(List<Card> cards) {
        for (int i = cards.size() - 1; i > 0; i--) {
            int k = rng.next(i + 1);
            if (k < 0 || k > i) throw new IllegalArgumentException("RNG inválido");
            Collections.swap(cards, i, k);
        }
    }
    private void note(String message) {
        lastAction = message;
        history.add(0, message);
        if (history.size() > 20) history.remove(history.size() - 1);
    }
    private void ensure(int player, String required) {
        if (winner != -1) throw new IllegalStateException("A partida terminou");
        if (player != currentPlayer) throw new IllegalArgumentException("Não é a vez desse jogador");
        if (!required.equals(phase)) throw new IllegalStateException("Primeiro " + ("draw".equals(required) ? "compre" : "compre ou jogue") + " uma carta");
    }

    public static int team(int player) { return player % 2; }
    public int currentPlayer() { return currentPlayer; }
    public String phase() { return phase; }
    public int winner() { return winner; }
    public int cardTotal() {
        int n = stock.size() + discard.size();
        for (List<Card> hand : hands) n += hand.size();
        for (List<Card> morto : mortos) n += morto.size();
        for (List<Card> meld : melds) n += meld.size();
        return n;
    }
    public int handSize(int p) { return hands[p].size(); }
    public boolean mortoTaken(int team) { return deadTaken[team]; }

    public void draw(int player, boolean pile) {
        ensure(player, "draw");
        if (pile) {
            if (discard.isEmpty()) throw new IllegalStateException("Lixeira vazia");
            hands[player].addAll(discard);
            discard.clear();
            note("Jogador " + (player + 1) + " pegou a lixeira.");
        } else {
            if (stock.isEmpty()) {
                if (discard.size() <= 1) {
                    finishByPoints();
                    return;
                }
                Card top = take(discard);
                stock.addAll(discard);
                discard.clear();
                discard.add(top);
                shuffle(stock);
            }
            hands[player].add(take(stock));
            note("Jogador " + (player + 1) + " comprou do monte.");
        }
        phase = "play";
    }

    public static Meld analyze(List<Card> cards) {
        Meld bad = new Meld();
        if (cards.size() < 3 || cards.size() > 13) return bad;
        int wild = 0, points = 0;
        List<Card> normals = new ArrayList<>();
        for (Card c : cards) {
            points += c.points();
            if (c.isWild()) wild++; else normals.add(c);
        }
        if (wild > 1 || normals.size() < 2) return bad;
        String suit = normals.get(0).suit;
        for (Card c : normals) if (!suit.equals(c.suit)) return bad;
        for (int aceHigh = 0; aceHigh <= 1; aceHigh++) {
            List<Integer> ranks = new ArrayList<>();
            for (Card c : normals) ranks.add(c.rank == 1 && aceHigh == 1 ? 14 : c.rank);
            Collections.sort(ranks);
            boolean duplicate = false;
            for (int i = 1; i < ranks.size(); i++) if (ranks.get(i).equals(ranks.get(i - 1))) duplicate = true;
            if (duplicate) continue;
            int min = ranks.get(0), max = ranks.get(ranks.size() - 1);
            // Um 2 curinga pode preencher lacuna interna ou ponta, jamais duplicar rank.
            int gap = max - min + 1 - normals.size();
            if (gap > wild) continue;
            int extra = wild - gap, first = min, last = max;
            while (extra > 0 && first > 3) { first--; extra--; }
            while (extra > 0 && last < 14) { last++; extra--; }
            if (extra > 0 || last - first + 1 != cards.size()) continue;
            Meld result = new Meld();
            result.valid = true;
            result.clean = wild == 0;
            result.canasta = cards.size() >= 7;
            result.first = first;
            result.last = last;
            result.suit = suit;
            result.points = points;
            return result;
        }
        return bad;
    }

    private List<Card> getSelected(int player, String csv) {
        if (csv == null || csv.isEmpty()) throw new IllegalArgumentException("Selecione cartas");
        String[] parts = csv.split(",");
        Set<Integer> unique = new HashSet<>();
        List<Card> chosen = new ArrayList<>();
        for (String part : parts) {
            int id;
            try { id = Integer.parseInt(part.trim()); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Carta inválida"); }
            if (!unique.add(id)) throw new IllegalArgumentException("Cartas repetidas");
            Card found = null;
            for (Card c : hands[player]) if (c.id == id) { found = c; break; }
            if (found == null) throw new IllegalArgumentException("Carta não encontrada na mão");
            chosen.add(found);
        }
        return chosen;
    }

    public void lay(int player, String csv, int existing) {
        ensure(player, "play");
        List<Card> chosen = getSelected(player, csv);
        int team = team(player);
        if (existing < -1 || existing >= melds[team].size()) throw new IllegalArgumentException("Jogo não encontrado");
        List<Card> full = new ArrayList<>();
        if (existing >= 0) full.addAll(melds[team].get(existing));
        full.addAll(chosen);
        Meld result = analyze(full);
        if (!result.valid) throw new IllegalArgumentException("Sequência inválida. Use 3+ cartas do mesmo naipe e no máximo um 2.");
        if (chosen.size() == hands[player].size() && deadTaken[team] && !hasCleanCanasta(team) && !(result.canasta && result.clean))
            throw new IllegalStateException("Para bater, sua equipe precisa de canastra limpa.");
        hands[player].removeAll(chosen);
        if (existing == -1) melds[team].add(full); else melds[team].set(existing, full);
        note("Jogador " + (player + 1) + " baixou " + chosen.size() + " carta(s).");
        if (hands[player].isEmpty()) noCards(player, true);
    }

    private boolean hasCleanCanasta(int team) {
        for (List<Card> cards : melds[team]) {
            Meld m = analyze(cards);
            if (m.valid && m.clean && m.canasta) return true;
        }
        return false;
    }

    private void noCards(int player, boolean withoutDiscard) {
        int team = team(player);
        if (!deadTaken[team]) {
            if (mortos[team].isEmpty()) throw new IllegalStateException("Morto indisponível");
            hands[player].addAll(mortos[team]);
            mortos[team].clear();
            deadTaken[team] = true;
            justGotDead[team] = withoutDiscard;
            note("Equipe " + (team == 0 ? "Solar" : "Lunar") + " pegou o morto!");
        } else {
            if (!hasCleanCanasta(team)) throw new IllegalStateException("É necessária canastra limpa para bater.");
            winner = team;
            phase = "finished";
            note("Batida! Vitória da Casa " + (team == 0 ? "Solar" : "Lunar") + "!");
        }
    }

    public void discard(int player, int id) {
        ensure(player, "play");
        Card card = null;
        for (Card c : hands[player]) if (c.id == id) { card = c; break; }
        if (card == null) throw new IllegalArgumentException("Selecione uma carta na mão");
        if (hands[player].size() == 1 && deadTaken[team(player)] && !hasCleanCanasta(team(player)))
            throw new IllegalStateException("É necessária uma canastra limpa antes de bater.");
        hands[player].remove(card);
        discard.add(card);
        note("Jogador " + (player + 1) + " descartou " + card.rank + card.suit + ".");
        if (hands[player].isEmpty()) noCards(player, false);
        if (winner == -1) nextTurn();
    }

    private void nextTurn() {
        currentPlayer = (currentPlayer + 1) % 4;
        turn++;
        phase = "draw";
        if (turn > 600) finishByPoints();
    }
    private void finishByPoints() {
        int a = score(0), b = score(1);
        winner = a == b ? 2 : a > b ? 0 : 1;
        phase = "finished";
        note("Fim das cartas: resultado por pontos.");
    }

    public int score(int team) {
        int total = deadTaken[team] ? 0 : -100;
        for (List<Card> cards : melds[team]) {
            Meld m = analyze(cards);
            if (m.valid) total += m.points + (m.canasta ? (m.clean ? 200 : 100) : 0);
        }
        for (int p = team; p < 4; p += 2) for (Card c : hands[p]) total -= c.points();
        if (winner == team) total += 100;
        return total;
    }

    private static final class Option {
        final List<Card> cards;
        final int existing;
        final int score;
        Option(List<Card> cards, int existing, int score) {
            this.cards = cards; this.existing = existing; this.score = score;
        }
    }

    private List<Option> options(int player) {
        List<Option> result = new ArrayList<>();
        List<Card> hand = hands[player];
        int team = team(player);
        // Estender jogos existentes.
        for (int i = 0; i < melds[team].size(); i++) {
            for (Card c : hand) {
                List<Card> combined = new ArrayList<>(melds[team].get(i));
                combined.add(c);
                Meld m = analyze(combined);
                if (m.valid) result.add(new Option(Collections.singletonList(c), i, 35 + (m.canasta ? 50 : 0)));
            }
        }
        // Encontrar sequências entre 3 e 9 cartas (estratégia bots).
        for (String suit : SUITS) {
            for (int start = 3; start <= 12; start++) {
                for (int end = start + 2; end <= 14 && end <= start + 8; end++) {
                    List<Card> choice = new ArrayList<>();
                    int missing = 0;
                    for (int rank = start; rank <= end; rank++) {
                        Card found = null;
                        for (Card c : hand)
                            if (!c.isWild() && c.suit.equals(suit) && (c.rank == rank || rank == 14 && c.rank == 1) && !choice.contains(c)) { found = c; break; }
                        if (found == null) missing++;
                        else choice.add(found);
                    }
                    if (missing > 1) continue;
                    if (missing == 1) for (Card c : hand) if (c.isWild()) { choice.add(c); break; }
                    if (choice.size() != end - start + 1) continue;
                    Meld m = analyze(choice);
                    if (!m.valid) continue;
                    int value = choice.size() * 8 + (m.canasta ? 70 : 0) + (m.clean ? 30 : 0);
                    result.add(new Option(choice, -1, value));
                }
            }
        }
        result.sort((a, b) -> Integer.compare(b.score, a.score));
        return result;
    }

    private int discardValue(int player, Card c) {
        if (c.isWild()) return 10000;
        int level = "especialista".equals(difficulty) ? 33 : "dificil".equals(difficulty) ? 24 : 13;
        int value = c.points();
        for (Card other : hands[player]) {
            if (other.id == c.id || other.isWild() || !other.suit.equals(c.suit)) continue;
            int d = Math.abs((c.rank == 1 ? 14 : c.rank) - (other.rank == 1 ? 14 : other.rank));
            if (d == 1) value += level;
            else if (d == 2) value += level / 2;
        }
        return value;
    }

    public void botStep() {
        if (currentPlayer == 0 || winner != -1) return;
        int player = currentPlayer;
        if ("draw".equals(phase)) {
            boolean takePile = false;
            if (!"facil".equals(difficulty) && !discard.isEmpty() && discard.size() < 8) {
                Card top = discard.get(discard.size() - 1);
                for (Card c : hands[player]) if (!c.isWild() && c.suit.equals(top.suit) && Math.abs(c.rank - top.rank) <= 2) takePile = true;
            }
            draw(player, takePile);
        }
        if (!"play".equals(phase)) return;
        int max = "facil".equals(difficulty) ? 1 : "normal".equals(difficulty) ? 4 : 10;
        for (int iteration = 0; iteration < max && "play".equals(phase); iteration++) {
            List<Option> possibilities = options(player);
            if (possibilities.isEmpty()) break;
            Option best = possibilities.get("facil".equals(difficulty) ? rng.next(possibilities.size()) : 0);
            if ("facil".equals(difficulty) && rng.next(100) < 45) break;
            StringBuilder ids = new StringBuilder();
            for (Card c : best.cards) { if (ids.length() > 0) ids.append(','); ids.append(c.id); }
            try { lay(player, ids.toString(), best.existing); }
            catch (IllegalArgumentException | IllegalStateException e) { break; }
        }
        if (!"play".equals(phase)) return;
        List<Card> candidate = new ArrayList<>(hands[player]);
        if (candidate.isEmpty()) return;
        if ("facil".equals(difficulty)) Collections.shuffle(candidate, new Random(rng.next(100000) + 1L));
        else candidate.sort(Comparator.comparingInt(c -> discardValue(player, c)));
        for (Card card : candidate) {
            try { discard(player, card.id); return; }
            catch (IllegalArgumentException | IllegalStateException e) { /* tente a próxima */ }
        }
        finishByPoints();
    }

    private static void appendString(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c == '\n') b.append("\\n");
            else if (c < 32) b.append(' ');
            else b.append(c);
        }
        b.append('"');
    }
    private static void cardsJson(StringBuilder b, List<Card> cards) {
        b.append('[');
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) b.append(',');
            Card c = cards.get(i);
            b.append("{\"id\":").append(c.id).append(",\"rank\":").append(c.rank).append(",\"suit\":");
            appendString(b, c.suit);
            b.append('}');
        }
        b.append(']');
    }
    public String snapshot() {
        StringBuilder b = new StringBuilder(3800);
        b.append("{\"player\":").append(currentPlayer).append(",\"turn\":").append(turn).append(",\"phase\":");
        appendString(b, phase);
        b.append(",\"difficulty\":"); appendString(b, difficulty);
        b.append(",\"winner\":").append(winner).append(",\"score\":[").append(score(0)).append(',').append(score(1)).append(']');
        b.append(",\"hands\":[");
        for (int i = 0; i < 4; i++) { if (i > 0) b.append(','); cardsJson(b, hands[i]); }
        b.append("],\"melds\":[");
        for (int t = 0; t < 2; t++) {
            if (t > 0) b.append(',');
            b.append('[');
            for (int k = 0; k < melds[t].size(); k++) {
                if (k > 0) b.append(',');
                cardsJson(b, melds[t].get(k));
            }
            b.append(']');
        }
        b.append("],\"stock\":").append(stock.size()).append(",\"discard\":");
        cardsJson(b, discard);
        b.append(",\"deadTaken\":[").append(deadTaken[0]).append(',').append(deadTaken[1]).append(']');
        b.append(",\"deadRemaining\":[").append(mortos[0].size()).append(',').append(mortos[1].size()).append(']');
        b.append(",\"lastAction\":"); appendString(b, lastAction);
        b.append(",\"history\":[");
        for (int i = 0; i < history.size(); i++) { if (i > 0) b.append(','); appendString(b, history.get(i)); }
        b.append("],\"cardTotal\":").append(cardTotal()).append('}');
        return b.toString();
    }
}
