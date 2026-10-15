package com.buracoarcano;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * QA COMBINATORIO REPRODUZIVEL.
 *
 * Cobertura exaustiva de todos os ternos (3 cartas) sem A/2 de dois baralhos,
 * sequencias validas e posicoes de curinga; complementada por partidas
 * completas pseudoaleatorias para abranger estados de 104 cartas.
 *
 * Cada caso executado recebe ID, parametros, resultado e seed em um CSV.
 * Executar:
 *   java -Dburaco.fuzzGames=64 -cp target/test-classes:target/classes
 *     com.buracoarcano.CombinatorialQa
 *   java -Dburaco.fuzzGames=500 -Dburaco.qaSeed=20261015
 *     -cp target/test-classes:target/classes com.buracoarcano.CombinatorialQa
 *
 * Obs: pseudoaleatoriedade reproduzivel somente na suite QA;
 * o jogo de navegador usa Web Crypto para embaralhar.
 */
public final class CombinatorialQa {
    private static final String[] LEVELS={"facil","normal","dificil","lendario"};
    private final long rootSeed;
    private final int gamesPerLevel;
    private final BufferedWriter out;
    private long count=0, fails=0;
    private final long[] counts=new long[7];
    private CombinatorialQa(long seed,int games,Path csv) throws IOException {
        rootSeed=seed;gamesPerLevel=games;
        Files.createDirectories(csv.getParent());
        out=Files.newBufferedWriter(csv,StandardCharsets.UTF_8);
        out.write("caso,categoria,seed,configuracao,resultado,detalhe\n");
    }
    private static String clean(String s) {
        return s.replace("\r"," ").replace("\n"," ").replace(",",";").replace("\"","'");
    }
    private void check(String id,String category,long seed,String config,boolean valid,String reason,int group) throws IOException {
        count++;counts[group]++;if(!valid)fails++;
        out.write(clean(id)+","+clean(category)+","+seed+","+clean(config)+","+(valid?"PASS":"FAIL")+","+clean(reason)+"\n");
        if(!valid){
            out.flush();
            throw new AssertionError("Falha "+id+" ["+category+"] "+config+": "+reason+
                ". Relatorio parcial escrito. Reproduzir com -Dburaco.qaSeed="+rootSeed);
        }
    }
    private static GameEngine.Card c(int copy,int suit,int rank){
        return new GameEngine.Card(copy,suit,rank);
    }
    private static String ids(List<GameEngine.Card> list){
        StringBuilder b=new StringBuilder();
        for(GameEngine.Card c:list){if(b.length()!=0)b.append('|');b.append(c.id);}
        return b.toString();
    }
    /** Todos os C(88,3)=109.736 ternos sem As/2: mesmo naipe e consecutivos = unico oraculo. */
    private void exhaustiveTriples() throws IOException {
        List<GameEngine.Card> pool=new ArrayList<>();
        for(int copy=0;copy<2;copy++)
            for(int suit=0;suit<4;suit++)
                for(int rank=3;rank<=13;rank++)pool.add(c(copy,suit,rank));
        int serial=0;
        for(int i=0;i<pool.size()-2;i++)
            for(int j=i+1;j<pool.size()-1;j++)
                for(int k=j+1;k<pool.size();k++){
                    GameEngine.Card a=pool.get(i),b=pool.get(j),z=pool.get(k);
                    int min=Math.min(a.rank,Math.min(b.rank,z.rank));
                    int max=Math.max(a.rank,Math.max(b.rank,z.rank));
                    boolean expected=a.suit==b.suit&&b.suit==z.suit &&
                        a.rank!=b.rank && b.rank!=z.rank && a.rank!=z.rank && max-min==2;
                    boolean actual=GameEngine.validate(Arrays.asList(a,b,z))!=null;
                    check(String.format(Locale.ROOT,"T3-%06d",++serial),"TERNO_EXAUSTIVO",rootSeed,
                        a.id+"|"+b.id+"|"+z.id,expected==actual,
                        "esperado="+expected+";obtido="+actual,0);
                }
        if(serial!=109736)throw new AssertionError("Catalogo de ternos deveria conter 109736 casos, veio "+serial);
    }
    /** Todas as posicoes de 1 curinga e comprimentos 3..11 em quatro naipes e duas copias. */
    private void validRunsAndJokers() throws IOException {
        int serial=0;
        for(int suit=0;suit<4;suit++)for(int length=3;length<=11;length++)
            for(int start=3;start+length-1<=13;start++)for(int copy=0;copy<2;copy++) {
                List<GameEngine.Card> natural=new ArrayList<>();
                for(int p=0;p<length;p++)natural.add(c(copy,suit,start+p));
                GameEngine.Run run=GameEngine.validate(natural);
                check(String.format(Locale.ROOT,"SEQ-%05d",++serial),"SEQUENCIA_NATURAL",rootSeed,
                    "naipe="+suit+";inicio="+start+";len="+length+";baralho="+copy,
                    run!=null&&run.clean&&run.ordered.size()==length,
                    "limpa=true",1);
                if(length>=7&&run!=null){
                    int bonus=GameEngine.meldBonus(new GameEngine.Meld(natural));
                    check(String.format(Locale.ROOT,"BON-%05d",serial),"PONTOS_CANASTRA",rootSeed,
                        "len="+length+";naipe="+suit+";baralho="+copy,
                        bonus==200,"bonus="+bonus,2);
                }
                for(int pos=0;pos<length;pos++){
                    List<GameEngine.Card> withWild=new ArrayList<>(natural);
                    withWild.set(pos,c(copy,(suit+1)%4,2));
                    GameEngine.Run dirty=GameEngine.validate(withWild);
                    int bonus=length>=7?GameEngine.meldBonus(new GameEngine.Meld(withWild)):0;
                    check(String.format(Locale.ROOT,"COR-%05d-%02d",serial,pos),"CURINGA_POSICAO",rootSeed,
                        "naipe="+suit+";inicio="+start+";len="+length+";pos="+pos+";baralho="+copy,
                        dirty!=null&&!dirty.clean&&dirty.wild==1&&(length<7||bonus==100),
                        "uma coringa fora do naipe;canastra suja="+(length>=7),1);
                }
            }
        for(int s=0;s<4;s++) {
            // Sequencias iniciadas em As e terminadas em As alto.
            List<GameEngine.Card> low=Arrays.asList(c(0,s,1),c(0,s,2),c(0,s,3));
            List<GameEngine.Card> high=Arrays.asList(c(0,s,12),c(0,s,13),c(0,s,1));
            check("ASE-BAIXO-"+s,"AS_EXTREMOS",rootSeed,"A-2-3 naipe="+s,
                GameEngine.validate(low)!=null&&GameEngine.validate(low).clean,"A baixo com 2 natural",1);
            check("ASE-ALTO-"+s,"AS_EXTREMOS",rootSeed,"Q-K-A naipe="+s,
                GameEngine.validate(high)!=null&&GameEngine.validate(high).clean,"A alto",1);
            List<GameEngine.Card> complete=new ArrayList<>();
            for(int rank=1;rank<=13;rank++)complete.add(c(0,s,rank));
            check("C13-"+s,"CANASTRA_13",rootSeed,"A-2-3-...-K naipe="+s,
                GameEngine.meldBonus(new GameEngine.Meld(complete))==500,
                "especial 13 cartas limpas",2);
            List<GameEngine.Card> doubleJoker=Arrays.asList(c(0,s,6),c(0,(s+1)%4,2),c(1,(s+2)%4,2));
            check("C2-"+s,"CURINGA_DUPLO",rootSeed,"naipe="+s,
                GameEngine.validate(doubleJoker)==null,"dois 2 fora de posicao natural",1);
        }
    }
    /** Avalia duas embaralhagens iguais e uma amostra de 4 niveis com maos, morto e monte. */
    private void dealsAndTurns() throws IOException {
        int serial=0;
        for(int level=0;level<LEVELS.length;level++)for(int seed=0;seed<512;seed++){
            long s=rootSeed+level*2000003L+seed;
            GameEngine g=new GameEngine(LEVELS[level],new Random(s)::nextInt);
            boolean shape=g.totalCards()==104 && g.stock.size()==37 && g.discardPile.size()==1;
            for(int p=0;p<4;p++)shape&=g.hands[p].size()==11;
            for(int dead=0;dead<2;dead++)shape&=g.deads[dead]!=null&&g.deads[dead].size()==11;
            shape&=unique(g);
            check(String.format(Locale.ROOT,"DIST-%05d",++serial),"DISTRIBUICAO",s,
                "bot="+LEVELS[level]+";indice="+seed,shape,
                "104 cartas;11 por jogador;11 por morto;37 compra;1 descarte",3);
            GameEngine copy=new GameEngine(LEVELS[level],new Random(s)::nextInt);
            check(String.format(Locale.ROOT,"SEED-%05d",serial),"REPRODUCAO",s,
                "bot="+LEVELS[level]+";indice="+seed,
                g.stateJson().equals(copy.stateJson()),
                "mesmo seed=>baralho e estado inicial identicos",3);
            check(String.format(Locale.ROOT,"FASE-%05d",serial),"PROIBICOES",s,
                "bot="+LEVELS[level]+";indice="+seed,
                !g.buyStock(1) && !g.buyDiscard(1) && !g.discard(0,g.hands[0].get(0).id) &&
                    !g.meld(0,g.hands[0].get(0).id,-1) && g.totalCards()==104,
                "nao permitir jogador errado nem descarte/baixa antes da compra",3);
        }
    }
    private static boolean unique(GameEngine g) {
        Set<String> seen=new HashSet<>();int n=0;
        for(GameEngine.Card c:g.stock){if(!seen.add(c.id))return false;n++;}
        for(GameEngine.Card c:g.discardPile){if(!seen.add(c.id))return false;n++;}
        for(List<GameEngine.Card> hand:g.hands)for(GameEngine.Card c:hand){
            if(!seen.add(c.id))return false;n++;
        }
        for(List<GameEngine.Card> dead:g.deads)if(dead!=null)for(GameEngine.Card c:dead){
            if(!seen.add(c.id))return false;n++;
        }
        for(List<GameEngine.Meld> set:g.melds)for(GameEngine.Meld m:set)for(GameEngine.Card c:m.cards){
            if(!seen.add(c.id))return false;n++;
        }
        return n==104&&g.totalCards()==104;
    }
    private void randomizedMatches() throws IOException {
        int cases=0;
        for(int lv=0;lv<LEVELS.length;lv++)for(int seed=0;seed<gamesPerLevel;seed++){
            long s=rootSeed+lv*501113L+seed*7919L;
            GameEngine g=new GameEngine(LEVELS[lv],new Random(s)::nextInt);
            int turns=0;boolean valid=true;String issue="";
            while(!g.stockEnded&&turns<220){
                int prior=g.player;
                try {
                    g.aiPlay(prior);
                    if(g.totalCards()!=104||!unique(g)) {
                        valid=false;issue="integridade de 104 cartas violada no turno="+turns;break;
                    }
                    if(!g.stockEnded&&g.player==prior) {
                        valid=false;issue="bot nao terminou o turno="+turns;break;
                    }
                }catch(Throwable e){valid=false;issue=e.getClass().getSimpleName()+":"+e.getMessage();break;}
                turns++;
            }
            if(turns>=220&&!g.stockEnded){valid=false;issue="possivel deadlock: 220 turnos";}
            check(String.format(Locale.ROOT,"SIM-%05d",++cases),"PARTIDA_INTEIRA",s,
                "nivel="+LEVELS[lv]+";jogadas="+turns,valid,
                valid?"104 cartas preservadas;sem excecoes;sem deadlock;termino="+g.stockEnded:issue,4);
        }
    }
    private void targetedIllegalMoves() throws IOException {
        // Reproduz, de forma isolada, os limites de cada estado do turno.
        for(int lv=0;lv<4;lv++)for(int seed=0;seed<256;seed++){
            long s=rootSeed+lv*33317L+seed;
            GameEngine g=new GameEngine(LEVELS[lv],new Random(s)::nextInt);
            boolean buy=g.buyStock(0);
            String first=g.hands[0].get(0).id;
            boolean refused=g.buyStock(0)==false&&g.buyDiscard(0)==false;
            boolean invalid=g.meld(0,first,-1)==false;
            boolean discard=g.discard(0,first);
            boolean blocked=g.discard(0,first)==false&&g.buyStock(0)==false;
            check("TURNO-"+lv+"-"+seed,"ESTADO_TURNO",s,
                "nivel="+LEVELS[lv],buy&&refused&&invalid&&discard&&blocked&&unique(g),
                "comprar 1x;nao baixar uma carta;descartar 1x;passar vez",5);
        }
    }
    private void invalidMeldInputs() throws IOException {
        for(int lv=0;lv<4;lv++)for(int seed=0;seed<128;seed++){
            long s=rootSeed+lv*1009L+seed*3L;
            GameEngine g=new GameEngine(LEVELS[lv],new Random(s)::nextInt);
            g.buyStock(0);
            String id=g.hands[0].get(0).id;
            String prior=g.stateJson();
            boolean result=!g.meld(0,"", -1)&&!g.meld(0,id+","+id,-1)
                &&!g.meld(0,"nao-existe", -1)&&!g.meld(0,id,9);
            check("INP-"+lv+"-"+seed,"ENTRADAS_INVALIDAS",s,"nivel="+LEVELS[lv],
                result&&prior.equals(g.stateJson()),"nao aceitar ids falsos, repetidos, indices invalidos",5);
        }
    }
    private void reportSummary() throws IOException {
        out.flush();
        System.out.println("QA COMBINATORIO concluido: "+count+" casos | falhas="+fails+
            " | seed="+rootSeed+" | partidas por nivel="+gamesPerLevel);
        System.out.println("Grupos: triplas="+counts[0]+", sequencias="+counts[1]+
            ", pontuacao="+counts[2]+", distribuicao="+counts[3]+", partidas="+counts[4]+
            ", estados="+counts[5]);
        if(fails>0)throw new AssertionError(fails+" casos falharam");
    }
    public static void main(String[] args) throws Exception {
        long root=Long.getLong("buraco.qaSeed",20261015L);
        int games=Integer.getInteger("buraco.fuzzGames",48);
        if(games<1||games>10000)throw new IllegalArgumentException("fuzzGames deve ser 1..10000");
        Path path=Path.of(System.getProperty("buraco.report","reports/qa-combinatorio.csv"));
        CombinatorialQa suite=new CombinatorialQa(root,games,path);
        try{
            suite.exhaustiveTriples();
            suite.validRunsAndJokers();
            suite.dealsAndTurns();
            suite.targetedIllegalMoves();
            suite.invalidMeldInputs();
            suite.randomizedMatches();
            suite.reportSummary();
        }finally{suite.out.close();}
    }
}
