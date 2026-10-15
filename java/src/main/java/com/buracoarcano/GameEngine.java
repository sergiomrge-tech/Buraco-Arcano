package com.buracoarcano;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Regras e bots da partida. Motor puro Java: não acessa DOM, HTML ou JavaScript.
 * Recebe a fonte de aleatoriedade do adaptador (WebCrypto no navegador).
 */
public final class GameEngine {
    public interface RandomSource { int nextInt(int bound); }
    public static final class Card {
        public final int copy, suit, rank;
        public final String id;
        public Card(int copy, int suit, int rank) {
            this.copy=copy;this.suit=suit;this.rank=rank;
            this.id=copy+"-"+suit+"-"+rank;
        }
    }
    public static final class Meld {
        public final List<Card> cards=new ArrayList<>();
        Meld(List<Card> source){cards.addAll(source);}
    }
    public static final class Run {
        public final int suit,start,wild;
        public final boolean clean;
        public final List<Card> ordered;
        Run(int suit,int start,int wild,List<Card> ordered) {
            this.suit=suit;this.start=start;this.wild=wild;this.ordered=ordered;this.clean=wild==0;
        }
    }
    public static final class Suggested {
        final List<Card> cards;
        final Run run;
        final int score;
        Suggested(List<Card> cards,Run run,int score){this.cards=cards;this.run=run;this.score=score;}
    }
    public final List<Card>[] hands;
    public final List<Meld>[] melds;
    public final List<Card>[] deads;
    public final List<Card> stock=new ArrayList<>(), discardPile=new ArrayList<>();
    public final int[] totals={0,0};
    public int[] lastRoundScore=null;
    public boolean[] deadTaken={false,false};
    public final List<String> logs=new ArrayList<>();
    public String message="",difficulty,phase;
    public int round=0,player=0;
    public Integer winner=null;
    public boolean stockEnded=false;
    private final RandomSource random;

    @SuppressWarnings("unchecked")
    public GameEngine(String difficulty,RandomSource random) {
        this(difficulty, random, true);
    }
    @SuppressWarnings("unchecked")
    private GameEngine(String difficulty,RandomSource random,boolean deal) {
        if(!Arrays.asList("facil","normal","dificil","lendario").contains(difficulty))throw new IllegalArgumentException("Dificuldade inválida");
        this.random=random;this.difficulty=difficulty;
        hands=(List<Card>[])new List<?>[4];
        deads=(List<Card>[])new List<?>[2];
        melds=(List<Meld>[])new List<?>[2];
        for(int i=0;i<4;i++)hands[i]=new ArrayList<>();
        for(int i=0;i<2;i++)melds[i]=new ArrayList<>();
        if(deal)newRound();
    }
    private void log(String text) {
        message=text;logs.add(0,text);
        if(logs.size()>35)logs.remove(logs.size()-1);
    }
    public static String playerName(int p){return new String[]{"Você","Morgath","Lyra","Valtor"}[p];}
    public static int teamOf(int p){return p%2;}
    public static int value(Card c){return c.rank==1?15:c.rank==2?20:c.rank>=8?10:5;}
    public static String name(Card c){
        String rank=c.rank==1?"A":c.rank==11?"J":c.rank==12?"Q":c.rank==13?"K":Integer.toString(c.rank);
        return rank+new String[]{"♠","♥","♦","♣"}[c.suit];
    }
    private static Card removeLast(List<Card> cards){return cards.remove(cards.size()-1);}
    public void newRound() {
        round++;stock.clear();discardPile.clear();
        for(List<Card> h:hands)h.clear();
        for(List<Meld> m:melds)m.clear();
        deadTaken=new boolean[]{false,false};
        player=0;phase="draw";winner=null;stockEnded=false;lastRoundScore=null;
        for(int copy=0;copy<2;copy++)for(int suit=0;suit<4;suit++)for(int rank=1;rank<=13;rank++)
            stock.add(new Card(copy,suit,rank));
        for(int i=stock.size()-1;i>0;i--) {
            int j=random.nextInt(i+1);
            if(j<0||j>i)throw new IllegalStateException("Fonte aleatória fora do intervalo");
            Card t=stock.get(i);stock.set(i,stock.get(j));stock.set(j,t);
        }
        for(int i=0;i<11;i++)for(int p=0;p<4;p++)hands[p].add(removeLast(stock));
        for(int m=0;m<2;m++) {
            deads[m]=new ArrayList<>();
            for(int i=0;i<11;i++)deads[m].add(removeLast(stock));
        }
        discardPile.add(removeLast(stock));
        log("Nova rodada: 104 cartas embaralhadas, 11 para cada jogador e dois mortos.");
    }
    public static Run validate(List<Card> cards) {
        int n=cards.size();
        if(n<3||n>14)return null;
        Set<String> unique=new HashSet<>();
        for(Card c:cards)if(c==null||c.rank<1||c.rank>13||c.suit<0||c.suit>3||!unique.add(c.id))return null;
        int suit=-1;
        for(Card c:cards)if(c.rank!=2) {
            if(suit<0)suit=c.suit;
            else if(suit!=c.suit)return null;
        }
        if(suit<0)return null;
        for(int start=1;start+n-1<=14;start++) {
            Card[] ordered=new Card[n];
            int wild=0;boolean invalid=false;
            List<Card> jokers=new ArrayList<>();
            for(Card c:cards) {
                if(c.rank==2) {jokers.add(c);continue;}
                int rank=c.rank;
                if(rank==1&&start+n-1==14) { if(start>1||ordered[0]!=null)rank=14; }
                int pos=rank-start;
                if(pos<0||pos>=n||ordered[pos]!=null) {invalid=true;break;}
                ordered[pos]=c;
            }
            if(invalid)continue;
            // Carta 2 do mesmo naipe é primeiro aproveitada em sua posição natural.
            int pos2=2-start;
            for(int i=0;i<jokers.size();i++) {
                Card c=jokers.get(i);
                if(c.suit==suit&&pos2>=0&&pos2<n&&ordered[pos2]==null) {
                    ordered[pos2]=c;jokers.remove(i);break;
                }
            }
            if(jokers.size()>1)continue;
            if(jokers.size()==1) {
                for(int i=0;i<n;i++)if(ordered[i]==null){ordered[i]=jokers.get(0);wild++;break;}
            }
            for(Card c:ordered)if(c==null){invalid=true;break;}
            if(!invalid)return new Run(suit,start,wild,new ArrayList<>(Arrays.asList(ordered)));
        }
        return null;
    }
    private boolean isTurn(int p,String wanted) {
        return winner==null && !stockEnded && p==player && wanted.equals(phase);
    }
    public boolean buyStock(int p) {
        if(!isTurn(p,"draw"))return false;
        if(stock.isEmpty())refillOrEnd();
        if(stock.isEmpty())return false;
        hands[p].add(removeLast(stock));phase="meld";
        log(playerName(p)+" comprou do monte.");return true;
    }
    public boolean buyDiscard(int p) {
        if(!isTurn(p,"draw")||discardPile.isEmpty())return false;
        int size=discardPile.size();
        hands[p].addAll(discardPile);discardPile.clear();phase="meld";
        log(playerName(p)+" recolheu "+size+" carta(s) do lixo.");return true;
    }
    public boolean meld(int p,String ids,int groupIndex) {
        if(!isTurn(p,"meld")||ids==null||ids.isEmpty())return false;
        String[] split=ids.split(",");
        List<Card> hand=hands[p],picked=new ArrayList<>();
        for(String id:split) {
            Card match=null;
            for(Card c:hand)if(c.id.equals(id)){match=c;break;}
            if(match==null||picked.contains(match))return false;
            picked.add(match);
        }
        int team=teamOf(p);
        if(groupIndex < -1 || groupIndex>=melds[team].size())return false;
        List<Card> combined=new ArrayList<>();
        if(groupIndex>=0)combined.addAll(melds[team].get(groupIndex).cards);
        combined.addAll(picked);
        Run run=validate(combined);
        if(run==null){log("Sequência inválida. São necessárias 3+ cartas consecutivas do mesmo naipe.");return false;}
        if(deadTaken[team]&&hand.size()==picked.size()&&!hasClean(team)&&!(combined.size()>=7&&run.clean)) {
            log("Sua dupla precisa ter uma canastra limpa antes da batida.");return false;
        }
        // Do not leave a sole card with neither a legal discard nor a legal finish.
        if(hand.size()-picked.size()==1&&deadTaken[team]) {
            Card last=null;for(Card c:hand)if(!picked.contains(c)){last=c;break;}
            List<List<Card>> future=new ArrayList<>();
            for(int i=0;i<melds[team].size();i++)future.add(i==groupIndex?combined:melds[team].get(i).cards);
            if(groupIndex==-1)future.add(combined);
            boolean clean=false,canLayLast=false;
            for(List<Card> m:future)if(m.size()>=7&&validate(m).clean)clean=true;
            for(List<Card> m:future){List<Card> ext=new ArrayList<>(m);ext.add(last);Run r=validate(ext);
                if(r!=null&&(clean||(ext.size()>=7&&r.clean)))canLayLast=true;}
            if(!(clean&&last.rank!=2)&&!canLayLast){log("Preserve mais uma carta até formar uma canastra limpa e uma saída válida.");return false;}
        }
        hand.removeAll(picked);
        if(groupIndex>=0){Meld m=melds[team].get(groupIndex);m.cards.clear();m.cards.addAll(run.ordered);}
        else melds[team].add(new Meld(run.ordered));
        log(playerName(p)+(groupIndex>=0?" ampliou um jogo.":" baixou "+picked.size()+" carta(s).")+
            (combined.size()>=7?" CANASTRA "+(run.clean?"LIMPA!":"SUJA!"):""));
        if(hand.isEmpty())onHandEmpty(p);
        return true;
    }
    public boolean discard(int p,String id) {
        if(!isTurn(p,"meld"))return false;
        List<Card> hand=hands[p];Card chosen=null;
        for(Card c:hand)if(c.id.equals(id)){chosen=c;break;}
        if(chosen==null)return false;
        int team=teamOf(p);
        if(hand.size()==1&&deadTaken[team]&&chosen.rank==2){log("Não é permitido bater descartando um 2.");return false;}
        if(hand.size()==1&&deadTaken[team]&&!hasClean(team)){
            log("É preciso uma canastra limpa para bater.");return false;
        }
        hand.remove(chosen);discardPile.add(chosen);
        log(playerName(p)+" descartou "+name(chosen)+".");
        if(hand.isEmpty()&&onHandEmpty(p))return true;
        player=(p+1)%4;phase="draw";
        if(stock.isEmpty())refillOrEnd();
        return true;
    }
    public boolean hasClean(int team) {
        for(Meld m:melds[team]) {
            Run run=validate(m.cards);
            if(m.cards.size()>=7&&run!=null&&run.clean)return true;
        }
        return false;
    }
    private boolean onHandEmpty(int p) {
        if(!hands[p].isEmpty())return false;
        int team=teamOf(p);
        if(!deadTaken[team]) {
            for(int i=0;i<2;i++)if(deads[i]!=null&&!deads[i].isEmpty()){
                hands[p].addAll(deads[i]);deads[i]=null;deadTaken[team]=true;
                log(playerName(p)+" pegou o MORTO da dupla!");return false;
            }
        }
        if(deadTaken[team]&&hasClean(team)){finishRound(team);return true;}
        // Se os dois mortos já foram utilizados ou viraram monte, não há como
        // recuperar a mão vazia da equipe que ainda não pegou morto.
        // Encerrar por pontos evita uma rodada irrecuperavelmente travada.
        if(!deadTaken[team] && (deads[0]==null||deads[0].isEmpty())
           && (deads[1]==null||deads[1].isEmpty())){
            finishRound(null);return true;
        }
        return false;
    }
    private void refillOrEnd() {
        if(!stock.isEmpty()||winner!=null||stockEnded)return;
        for(int i=0;i<2;i++)if(deads[i]!=null&&!deads[i].isEmpty()){
            stock.addAll(deads[i]);deads[i]=null;
            log("O morto restante virou monte de compras.");
            return;
        }
        finishRound(null);
    }
    private static int sum(List<Card> cards){int result=0;for(Card c:cards)result+=value(c);return result;}
    public static int meldBonus(Meld m) {
        if(m.cards.size()<7)return 0;
        Run run=validate(m.cards);
        if(run==null)return 0;
        if(!run.clean)return 100;
        if(m.cards.size()>=14)return 1000;
        if(m.cards.size()>=13)return 500;
        return 200;
    }
    private void finishRound(Integer teamWinner) {
        if(stockEnded||winner!=null)return;
        stockEnded=true;winner=teamWinner;phase="end";
        int[] result={0,0};
        for(int t=0;t<2;t++) {
            for(Meld m:melds[t])result[t]+=sum(m.cards)+meldBonus(m);
            for(int p=t;p<4;p+=2)result[t]-=sum(hands[p]);
            if(!deadTaken[t])result[t]-=100;
            if(teamWinner!=null&&teamWinner==t)result[t]+=100;
            totals[t]+=result[t];
        }
        lastRoundScore=result;
        log((teamWinner==null?"Monte esgotado — rodada encerrada.":(teamWinner==0?"Aurora":"Sombra")+" bateram!")+
            " Pontos: "+result[0]+" x "+result[1]);
    }
    public Suggested bestMeld(List<Card> hand,int budget) {
        Suggested best=null;int checked=0,size=Math.min(hand.size(),42);
        outer:
        for(int i=0;i<size-2;i++)for(int j=i+1;j<size-1;j++)for(int k=j+1;k<size;k++){
            if(++checked>budget)break outer;
            List<Card> group=new ArrayList<>(Arrays.asList(hand.get(i),hand.get(j),hand.get(k)));
            Run run=validate(group);
            if(run==null)continue;
            while(group.size()<14) {
                Card found=null;Run bestRun=null;
                for(Card c:hand) {
                    if(group.contains(c))continue;
                    List<Card> expanded=new ArrayList<>(group);expanded.add(c);
                    Run attempt=validate(expanded);
                    if(attempt!=null&&(found==null||(!bestRun.clean&&attempt.clean))) {found=c;bestRun=attempt;}
                }
                if(found==null)break;
                group.add(found);run=bestRun;
            }
            int score=group.size()*13+(run.clean?6:0)+(group.size()>=7?110:0);
            if(best==null||score>best.score)best=new Suggested(group,run,score);
        }
        return best;
    }
    private int rand(int n){return random.nextInt(n);}
    private boolean wantsDiscard(int p) {
        if(discardPile.isEmpty())return false;
        if("facil".equals(difficulty))return rand(10)<2&&discardPile.size()<=3;
        if(discardPile.size()>=9&&"normal".equals(difficulty))return false;
        Card last=discardPile.get(discardPile.size()-1);
        List<Card> augmented=new ArrayList<>(hands[p]);augmented.add(last);
        Suggested s=bestMeld(augmented,7000);boolean useful=s!=null&&s.cards.contains(last);
        for(Meld m:melds[teamOf(p)]) {
            List<Card> extended=new ArrayList<>(m.cards);extended.add(last);
            if(validate(extended)!=null)useful=true;
        }
        int gain=useful?50:0;
        int threshold="normal".equals(difficulty)?55:"dificil".equals(difficulty)?43:33;
        return gain+Math.min(12,discardPile.size()*2)>=threshold;
    }
    public void aiPlay(int p) {
        if(player!=p||winner!=null||stockEnded)return;
        int team=teamOf(p);boolean hard="dificil".equals(difficulty)||"lendario".equals(difficulty);
        if("draw".equals(phase)) {
            if(wantsDiscard(p))buyDiscard(p);else buyStock(p);
        }
        if(!"meld".equals(phase)||winner!=null)return;
        for(int tries=0;tries<18&&!hands[p].isEmpty()&&winner==null;tries++) {
            Card extension=null;int index=-1;int extensionScore=0;
            for(int i=0;i<melds[team].size()&&extension==null;i++) {
                for(Card c:hands[p]) {
                    List<Card> run=new ArrayList<>(melds[team].get(i).cards);run.add(c);
                    if(validate(run)!=null){extension=c;index=i;extensionScore=25+(run.size()==7?120:0);break;}
                }
            }
            Suggested fresh=bestMeld(hands[p],hard?30000:7000);
            List<Card> take=null;
            if(fresh!=null&&(extension==null||fresh.score>extensionScore)){take=fresh.cards;index=-1;}
            else if(extension!=null)take=Arrays.asList(extension);
            if(take==null)break;
            if("facil".equals(difficulty)&&rand(10)<4)break;
            int before=hands[p].size();
            StringBuilder ids=new StringBuilder();
            for(Card c:take) {if(ids.length()>0)ids.append(",");ids.append(c.id);}
            if(!meld(p,ids.toString(),index)||hands[p].size()==before)break;
            if(!"meld".equals(phase)||winner!=null)return;
        }
        if(hands[p].isEmpty())return;
        if(hands[p].size()==1&&deadTaken[team]&&!hasClean(team)) {
            // Não descarta ilegalmente; conserva a carta enquanto a dupla busca a canastra limpa.
            log(playerName(p)+" preservou a última carta.");player=(p+1)%4;phase="draw";return;
        }
        Card choice=hands[p].get(0);
        if("facil".equals(difficulty))choice=hands[p].get(rand(hands[p].size()));
        else {
            int lowest=Integer.MAX_VALUE;
            for(Card c:hands[p]) {
                int score=value(c)/3+(c.rank==2?42:0);
                for(Card other:hands[p])if(other!=c){
                    if(other.suit==c.suit&&Math.abs(other.rank-c.rank)<=2)score+=hard?18:13;
                    if(other.suit==c.suit&&other.rank==c.rank)score+=4;
                }
                if(hard)for(Meld m:melds[team]){
                    List<Card> m2=new ArrayList<>(m.cards);m2.add(c);
                    if(validate(m2)!=null)score+=75;
                }
                if("lendario".equals(difficulty))for(int i=Math.max(0,discardPile.size()-5);i<discardPile.size();i++) {
                    Card last=discardPile.get(i);
                    if(last.suit==c.suit&&Math.abs(last.rank-c.rank)<=2)score+=14;
                }
                if(score<lowest){lowest=score;choice=c;}
            }
        }
        discard(p,choice.id);
    }
    public String hintJson() {
        if(player!=0||!"meld".equals(phase)||stockEnded)return "null";
        for(int i=0;i<melds[0].size();i++)for(Card c:hands[0]) {
            List<Card> cards=new ArrayList<>(melds[0].get(i).cards);cards.add(c);
            if(validate(cards)!=null)return "{\"ids\":["+quote(c.id)+"],\"index\":"+i+"}";
        }
        Suggested best=bestMeld(hands[0],30000);
        if(best==null)return "null";
        StringBuilder b=new StringBuilder("{\"ids\":[");
        for(Card c:best.cards){if(b.length()>8)b.append(',');b.append(quote(c.id));}
        return b.append("],\"index\":-1}").toString();
    }
    private static String zone(List<Card> cards) {
        if(cards==null)return "-";
        StringBuilder b=new StringBuilder();for(Card c:cards){if(b.length()>0)b.append(',');b.append(c.id);}return b.toString();
    }
    public String checkpoint() {
        StringBuilder b=new StringBuilder("BA1|").append(round).append('|').append(player).append('|').append(phase).append('|').append(difficulty)
          .append('|').append(totals[0]).append('|').append(totals[1]).append('|').append(deadTaken[0]?1:0).append('|').append(deadTaken[1]?1:0)
          .append('|').append(stockEnded?1:0).append('|').append(winner==null?-1:winner)
          .append('|').append(lastRoundScore==null?"-":lastRoundScore[0]+","+lastRoundScore[1]);
        for(List<Card> h:hands)b.append('\n').append(zone(h));
        for(List<Card> d:deads)b.append('\n').append(zone(d));
        b.append('\n').append(zone(stock)).append('\n').append(zone(discardPile));
        for(List<Meld> set:melds){b.append('\n');for(int i=0;i<set.size();i++){if(i>0)b.append(';');b.append(zone(set.get(i).cards));}}
        return b.toString();
    }
    private static List<Card> parseZone(String data,Set<String> seen) {
        if(data.equals("-"))return null;
        List<Card> cards=new ArrayList<>();if(data.isEmpty())return cards;
        for(String id:data.split(",",-1)){
            String[] bits=id.split("-",-1);if(bits.length!=3)throw new IllegalArgumentException("Save inválido");
            int copy=Integer.parseInt(bits[0]),suit=Integer.parseInt(bits[1]),rank=Integer.parseInt(bits[2]);
            Card c=new Card(copy,suit,rank);
            if(copy<0||copy>1||suit<0||suit>3||rank<1||rank>13||!c.id.equals(id)||!seen.add(id))throw new IllegalArgumentException("Cartas inválidas no save");
            cards.add(c);
        }return cards;
    }
    public static GameEngine restore(String text,RandomSource random) {
        if(text==null||text.length()>24000)throw new IllegalArgumentException("Save inválido");
        String[] lines=text.split("\n",-1);if(lines.length!=11)throw new IllegalArgumentException("Save incompleto");
        String[] head=lines[0].split("\\|",-1);if(head.length!=12||!head[0].equals("BA1"))throw new IllegalArgumentException("Versão inválida");
        GameEngine g=new GameEngine(head[4],random,false);g.round=Integer.parseInt(head[1]);g.player=Integer.parseInt(head[2]);g.phase=head[3];
        if(g.round<1||g.round>3||g.player<0||g.player>3||!Arrays.asList("draw","meld","end").contains(g.phase))throw new IllegalArgumentException("Turno inválido");
        g.totals[0]=Integer.parseInt(head[5]);g.totals[1]=Integer.parseInt(head[6]);
        for(int i=7;i<=9;i++)if(!head[i].equals("0")&&!head[i].equals("1"))throw new IllegalArgumentException("Flag inválida");
        g.deadTaken=new boolean[]{head[7].equals("1"),head[8].equals("1")};g.stockEnded=head[9].equals("1");
        int w=Integer.parseInt(head[10]);if(w< -1||w>1)throw new IllegalArgumentException("Vencedor inválido");g.winner=w<0?null:w;
        if(!head[11].equals("-")){String[] v=head[11].split(",");if(v.length!=2)throw new IllegalArgumentException("Placar inválido");g.lastRoundScore=new int[]{Integer.parseInt(v[0]),Integer.parseInt(v[1])};}
        if(g.stockEnded!=g.phase.equals("end")||g.stockEnded!=(g.lastRoundScore!=null)||(!g.stockEnded&&g.winner!=null))throw new IllegalArgumentException("Estado final inválido");
        Set<String> seen=new HashSet<>();int row=1;
        for(List<Card> hand:g.hands){List<Card> v=parseZone(lines[row++],seen);if(v==null)throw new IllegalArgumentException("Mão inválida");hand.addAll(v);}
        for(int i=0;i<2;i++){g.deads[i]=parseZone(lines[row++],seen);if(g.deads[i]!=null&&g.deads[i].size()!=11)throw new IllegalArgumentException("Morto inválido");}
        List<Card> stock=parseZone(lines[row++],seen),discard=parseZone(lines[row++],seen);if(stock==null||discard==null)throw new IllegalArgumentException("Monte inválido");
        g.stock.addAll(stock);g.discardPile.addAll(discard);
        for(int t=0;t<2;t++){String line=lines[row++];if(!line.isEmpty())for(String part:line.split(";",-1)){List<Card> m=parseZone(part,seen);if(m==null||validate(m)==null)throw new IllegalArgumentException("Sequência inválida");g.melds[t].add(new Meld(m));}}
        if(seen.size()!=104)throw new IllegalArgumentException("Save não conserva 104 cartas");
        for(int p=0;p<4;p++)if(!g.stockEnded&&g.hands[p].isEmpty())throw new IllegalArgumentException("Mão vazia inválida");
        g.log("Partida retomada do salvamento local.");return g;
    }
    public int totalCards() {
        int n=stock.size()+discardPile.size();
        for(List<Card> hand:hands)n+=hand.size();
        for(List<Card> dead:deads)if(dead!=null)n+=dead.size();
        for(List<Meld> set:melds)for(Meld m:set)n+=m.cards.size();
        return n;
    }
    private static void appendCards(StringBuilder sb,List<Card> cards) {
        sb.append('[');
        if(cards!=null)for(int i=0;i<cards.size();i++) {
            if(i>0)sb.append(',');
            Card c=cards.get(i);
            sb.append("{\"id\":\"").append(c.id).append("\",\"s\":").append(c.suit)
              .append(",\"r\":").append(c.rank).append('}');
        }
        sb.append(']');
    }
    private static String quote(String str) {
        StringBuilder b=new StringBuilder("\"");
        for(int i=0;i<str.length();i++){
            char ch=str.charAt(i);
            switch(ch) {
                case '"': b.append("\\\"");break;
                case '\\': b.append("\\\\");break;
                case '\n': b.append("\\n");break;
                case '\r': b.append("\\r");break;
                case '\t': b.append("\\t");break;
                default:b.append(ch);
            }
        }
        return b.append('"').toString();
    }
    public String stateJson() {
        StringBuilder sb=new StringBuilder(13000);
        sb.append("{\"round\":").append(round).append(",\"difficulty\":").append(quote(difficulty))
          .append(",\"player\":").append(player).append(",\"phase\":").append(quote(phase))
          .append(",\"winner\":").append(stockEnded?(winner==null?"\"stock\"":winner):"null")
          .append(",\"message\":").append(quote(message))
          .append(",\"stock\":");appendCards(sb,stock);
        sb.append(",\"discardPile\":");appendCards(sb,discardPile);
        sb.append(",\"hands\":[");
        for(int i=0;i<4;i++){if(i>0)sb.append(',');appendCards(sb,hands[i]);}
        sb.append("],\"deads\":[");
        for(int i=0;i<2;i++){if(i>0)sb.append(',');if(deads[i]==null)sb.append("null");else appendCards(sb,deads[i]);}
        sb.append("],\"melds\":[");
        for(int t=0;t<2;t++) {
            if(t>0)sb.append(',');sb.append('[');
            for(int i=0;i<melds[t].size();i++) {
                if(i>0)sb.append(',');sb.append("{\"cards\":");appendCards(sb,melds[t].get(i).cards);sb.append('}');
            }
            sb.append(']');
        }
        sb.append("],\"deadTaken\":[").append(deadTaken[0]).append(',').append(deadTaken[1]).append(']');
        sb.append(",\"totals\":[").append(totals[0]).append(',').append(totals[1]).append(']');
        sb.append(",\"lastRoundScore\":");
        if(lastRoundScore==null)sb.append("null");
        else sb.append('[').append(lastRoundScore[0]).append(',').append(lastRoundScore[1]).append(']');
        sb.append(",\"logs\":[");for(int i=0;i<logs.size();i++){if(i>0)sb.append(',');sb.append(quote(logs.get(i)));}
        sb.append("],\"totalCards\":").append(totalCards()).append('}');
        return sb.toString();
    }
}
