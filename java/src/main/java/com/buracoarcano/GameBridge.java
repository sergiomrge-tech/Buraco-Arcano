package com.buracoarcano;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;

/** Interface Java -> navegador; todas as regras são executadas no motor Java. */
public final class GameBridge {
    private static Game active;
    private GameBridge() {}

    @JSBody(params = {"bound"}, script =
        "if (!globalThis.crypto || !globalThis.crypto.getRandomValues) " +
        "throw new Error('Esta página precisa de Web Crypto (HTTPS ou localhost).');" +
        "if (!Number.isInteger(bound) || bound < 1) throw new Error('Limite inválido');" +
        "const arr = new Uint32Array(1);" +
        "const limit = Math.floor(4294967296 / bound) * bound;" +
        "do { globalThis.crypto.getRandomValues(arr); } while (arr[0] >= limit);" +
        "return arr[0] % bound;")
    private static native int secureIndex(int bound);

    @JSExport
    public static String start(String difficulty) {
        try {
            active = new Game(difficulty, GameBridge::secureIndex);
            return active.snapshot();
        } catch (Exception e) { return error(e); }
    }

    @JSExport
    public static String state() {
        return active == null ? start("normal") : active.snapshot();
    }

    @JSExport
    public static String drawStock() { return run(() -> active.draw(0, false)); }

    @JSExport
    public static String drawDiscard() { return run(() -> active.draw(0, true)); }

    @JSExport
    public static String lay(String cardIds, int existingMeld) {
        return run(() -> active.lay(0, cardIds, existingMeld));
    }

    @JSExport
    public static String discard(int cardId) {
        return run(() -> active.discard(0, cardId));
    }

    @JSExport
    public static String botStep() { return run(() -> active.botStep()); }

    private static String run(Runnable action) {
        if (active == null) return start("normal");
        try { action.run(); return active.snapshot(); }
        catch (Exception e) { return error(e); }
    }

    private static String error(Exception ex) {
        String message = ex.getMessage() == null ? "Erro desconhecido" : ex.getMessage();
        StringBuilder b = new StringBuilder("{\"error\":\"");
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '"' || c == '\\') b.append('\\');
            if (c >= 32) b.append(c);
        }
        return b.append("\"}").toString();
    }
}
