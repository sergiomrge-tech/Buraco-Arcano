package com.buracoarcano;

import java.util.ArrayList;
import java.util.List;

/** Morto por dupla, reciclagem do morto como monte, batida e encerramento. */
public final class DeadPileAndExitTest {
    private static GameEngine.Card extract(GameEngine g,int suit,int rank) {
        List<List<GameEngine.Card>> pools=new ArrayList<>();
        pools.add(g.stock);pools.add(g.discardPile);
        pools.add(g.hands[1]);pools.add(g.hands[2]);pools.add(g.hands[3]);
        for(List<GameEngine.Card> d:g.deads)if(d!=null)pools.add(d);
        for(List<GameEngine.Card> list:pools)
            for(int i=0;i<list.size();i++){
                GameEngine.Card c=list.get(i);
                if(c.suit==suit&&c.rank==rank){list.remove(i);return c;}
            }
        throw new AssertionError("Não encontrou carta para construir teste "+suit+"/"+rank);
    }
    public static long run(){
        long cases=0;
        for(int seed=0;seed<128;seed++){
            GameEngine g=TestSupport.game(seed,"normal");
            g.stock.addAll(g.hands[0]);g.hands[0].clear();
            TestSupport.check(g.buyStock(0),"compra antes de pegar morto");cases++;
            String last=g.hands[0].get(0).id;
            TestSupport.check(g.discard(0,last),"descarte que pega morto");cases++;
            TestSupport.check(g.deadTaken[0]&&g.hands[0].size()==11,"morto deveria voltar à mão");cases++;
            TestSupport.check(g.deads[0]==null,"morto usado deve desaparecer");cases++;
            TestSupport.invariant(g,"morto adquirido");
            while(g.hands[0].size()>1)g.stock.add(g.hands[0].remove(g.hands[0].size()-1));
            g.player=0;g.phase="meld";
            TestSupport.check(!g.discard(0,g.hands[0].get(0).id),"bateu sem limpa");cases++;
            TestSupport.invariant(g,"batida bloqueada");

            List<GameEngine.Card> clean=new ArrayList<>();
            for(int rank=3;rank<=9;rank++)clean.add(extract(g,0,rank));
            g.melds[0].add(new GameEngine.Meld(clean));
            TestSupport.check(g.hasClean(0),"canastra limpa preparada");cases++;
            TestSupport.invariant(g,"limpa construída");
            TestSupport.check(g.discard(0,g.hands[0].get(0).id),"batida com limpa deve funcionar");cases++;
            TestSupport.check(g.stockEnded&&g.winner!=null&&g.winner==0&&g.phase.equals("end"),
                "rodada deveria terminar com vitória Solar");cases++;
            TestSupport.invariant(g,"batida final");
            TestSupport.check(g.lastRoundScore!=null,"placar final ausente");cases++;
        }
        // Uso do morto sobrante para recompor o monte após esgotamento.
        for(int seed=300;seed<350;seed++){
            GameEngine g=TestSupport.game(seed,"normal");
            g.hands[3].addAll(g.stock);g.stock.clear();
            TestSupport.check(g.buyStock(0),"monte deveria ser abastecido por morto");cases++;
            TestSupport.check(g.stock.size()==10,"11 do morto - 1 comprada");cases++;
            TestSupport.invariant(g,"reciclagem de morto");
        }
        // Se ambos os mortos e monte acabarem, a rodada deve encerrar por pontuação.
        for(int seed=400;seed<450;seed++){
            GameEngine g=TestSupport.game(seed,"normal");
            g.hands[3].addAll(g.stock);g.stock.clear();
            for(int d=0;d<2;d++){
                g.hands[3].addAll(g.deads[d]);
                g.deads[d]=null;
            }
            TestSupport.check(!g.buyStock(0),"compra impossível com monte e mortos esgotados");cases++;
            TestSupport.check(g.stockEnded&&g.phase.equals("end"),"rodada não acabou");cases++;
            TestSupport.invariant(g,"fim do monte");
        }
        // Regressão: um jogador esvazia a mão depois de os dois mortos
        // terem sido consumidos como monte. Antes o bot ficava travado.
        for(int seed=500;seed<550;seed++){
            GameEngine g=TestSupport.game(seed,"normal");
            for(int d=0;d<2;d++){
                g.hands[3].addAll(g.deads[d]);g.deads[d]=null;
            }
            g.stock.addAll(g.hands[0]);g.hands[0].clear();
            TestSupport.check(g.buyStock(0),"compra para testar mão sem mortos");cases++;
            String card=g.hands[0].get(0).id;
            TestSupport.check(g.discard(0,card),"descarte sem morto");cases++;
            TestSupport.check(g.stockEnded&&g.phase.equals("end"),
                "mão vazia sem mortos deve encerrar rodada por pontos");cases++;
            TestSupport.invariant(g,"regressão sem mortos");
        }
        TestSupport.okay("Morto, canastra obrigatória, batida e monte esgotado",cases);
        return cases;
    }
    public static void main(String[] args){run();}
}
