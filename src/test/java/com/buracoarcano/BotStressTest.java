package com.buracoarcano;

import java.util.Random;

/**
 * Fuzz determinístico de rodadas 2v2 em quatro dificuldades.
 * Valida conservação, unicidade, sequências, progresso e repetibilidade.
 */
public final class BotStressTest {
    private static final String[] LEVELS={"facil","normal","dificil","lendario"};

    private static long play(long seed,String level,boolean checkReplay) {
        Random random=new Random(seed);
        GameEngine g=new GameEngine(level,random::nextInt);
        GameEngine replay=checkReplay?new GameEngine(level,new Random(seed)::nextInt):null;
        int turns=0;
        TestSupport.invariant(g,"início "+level+" / "+seed);
        while(!g.stockEnded&&turns<250){
            int before=g.player;
            g.aiPlay(before);
            TestSupport.invariant(g,level+" / "+seed+" / turno "+turns);
            if(!g.stockEnded)TestSupport.check(g.player!=before,
                "bot ficou preso no turno "+turns+" do jogador "+before+" nível "+level+" semente "+seed);
            if(replay!=null){
                replay.aiPlay(replay.player);
                TestSupport.check(g.stateJson().equals(replay.stateJson()),
                    "replay divergente: "+level+" / "+seed+" / turno "+turns);
            }
            turns++;
        }
        TestSupport.check(g.stockEnded,"rodada não terminou em 250 turnos: "+level+"/"+seed);
        TestSupport.check(g.lastRoundScore!=null,"placar final ausente "+level+"/"+seed);
        TestSupport.check(g.phase.equals("end"),"fase final errada "+level+"/"+seed);
        return turns;
    }
    public static long run(){
        long turns=0, games=0;
        for(String level:LEVELS)for(int i=0;i<18;i++){
            turns+=play(100_900L+i*104729L+games*1303L,level,i<4);
            games++;
        }
        System.out.println("  Rodadas concluídas="+games+" | turnos verificados="+turns);
        TestSupport.okay("Fuzz de quatro bots, partidas completas e replay",games);
        return games;
    }
    public static void main(String[] args){run();}
}
