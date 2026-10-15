package com.buracoarcano;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;

/** Interface pública mínima entre o motor Java/TeaVM e a mesa HTML. */
public final class BrowserBridge {
    private static GameEngine engine;

    private BrowserBridge() {}

    /** Web Crypto, com rejeição para remover viés na seleção de índices. */
    @JSBody(params={"bound"},script="var values = new Uint32Array(1);"
       +"var span = 4294967296; var limit = Math.floor(span / bound) * bound;"
       +"do { crypto.getRandomValues(values); } while (values[0] >= limit);"
       +"return values[0] % bound;")
    public static native int cryptoUniform(int bound);

    @JSExport
    public static void newGame(String difficulty) {
        engine=new GameEngine(difficulty,BrowserBridge::cryptoUniform);
    }
    @JSExport
    public static void newRound() {engine.newRound();}
    @JSExport
    public static String state() {return engine.stateJson();}
    @JSExport
    public static boolean buyStock(int player) {return engine.buyStock(player);}
    @JSExport
    public static boolean buyDiscard(int player) {return engine.buyDiscard(player);}
    @JSExport
    public static boolean meld(int player,String ids,int groupIndex) {return engine.meld(player,ids,groupIndex);}
    @JSExport
    public static boolean discard(int player,String id) {return engine.discard(player,id);}
    @JSExport
    public static void aiPlay(int player) {engine.aiPlay(player);}
    @JSExport
    public static int totalCards() {return engine.totalCards();}
    @JSExport
    public static String checkpoint() {return engine.checkpoint();}
    @JSExport
    public static boolean restore(String text) {try{engine=GameEngine.restore(text,BrowserBridge::cryptoUniform);return true;}catch(Exception e){return false;}}
    @JSExport
    public static void difficulty(String level) {if(java.util.Arrays.asList("facil","normal","dificil","lendario").contains(level))engine.difficulty=level;}
    @JSExport
    public static String hint() {return engine.hintJson();}
    @JSExport
    public static String analyze(String ids) {
        java.util.List<GameEngine.Card> cards=new java.util.ArrayList<>();
        try{for(String id:ids.split(",")){String[] v=id.split("-");cards.add(new GameEngine.Card(Integer.parseInt(v[0]),Integer.parseInt(v[1]),Integer.parseInt(v[2])));}}catch(Exception e){return "null";}
        GameEngine.Run r=GameEngine.validate(cards);if(r==null)return "null";
        return "{\"clean\":"+r.clean+",\"bonus\":"+GameEngine.meldBonus(new GameEngine.Meld(r.ordered))+"}";
    }
    public static void main(String[] args) {}
}
