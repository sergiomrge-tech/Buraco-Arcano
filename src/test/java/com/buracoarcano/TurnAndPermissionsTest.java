package com.buracoarcano;

/** Proíbe compra dupla, turno alheio, descarte precoce e IDs inexistentes. */
public final class TurnAndPermissionsTest {
    public static long run(){
        long checks=0;
        for(long seed=0;seed<350;seed++){
            GameEngine g=TestSupport.game(seed,"dificil");
            String id=g.hands[0].get(0).id;
            TestSupport.check(!g.buyStock(1),"rival tentou comprar no turno humano");checks++;
            TestSupport.check(!g.buyDiscard(2),"parceira tentou comprar fora da vez");checks++;
            TestSupport.check(!g.discard(0,id),"descartou antes de comprar");checks++;
            TestSupport.check(!g.meld(0,id,-1),"baixou antes de comprar");checks++;
            TestSupport.check(g.buyStock(0),"compra humana deveria funcionar");checks++;
            TestSupport.check(!g.buyStock(0),"dupla compra no turno");checks++;
            TestSupport.check(!g.buyDiscard(0),"compra do lixo após comprar do monte");checks++;
            TestSupport.check(!g.discard(1,g.hands[1].get(0).id),"rival descartou no turno alheio");checks++;
            TestSupport.check(!g.meld(0,"carta-inexistente",-1),"id inexistente aceito");checks++;
            TestSupport.check(!g.meld(0,id+","+id,-1),"id repetido aceito");checks++;
            TestSupport.check(!g.meld(0,id,10),"jogo de índice inválido aceito");checks++;
            TestSupport.check(g.discard(0,id),"descarte humano falhou");checks++;
            TestSupport.check(g.player==1&&g.phase.equals("draw"),"turno deveria passar a jogador 1");checks++;
            TestSupport.check(!g.discard(0,g.hands[0].get(0).id),"descarte duplo aceito");checks++;
            TestSupport.invariant(g,"após compra/descarte "+seed);
        }
        TestSupport.okay("Turnos, fases e permissões",checks);
        return checks;
    }
    public static void main(String[] args){run();}
}
