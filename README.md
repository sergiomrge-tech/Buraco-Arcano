# Buraco Arcano — As Quatro Casas

Jogo de Buraco aberto para navegador/Android, com mesa vertical de fantasia e regras executadas em **Java 17 compilado para JavaScript com TeaVM**. Integração v0.2; partida local contra três bots. Projeto completo ainda depende de multiplayer e homologação visual.

## Executar

```sh
mvn -f java/pom.xml process-classes -DskipTests
npm run check
npm run build
python3 -m http.server 8000 --directory dist
```

Abra `http://localhost:8000`. Em hospedagem, use HTTPS para Web Crypto e PWA. No Android, use Adicionar à tela inicial/Instalar aplicativo no menu do Chrome.

## Jogar

Toque no monte ou no lixo para comprar. Selecione três ou mais cartas para baixar sequência; para ampliar uma sequência, selecione as cartas e toque no jogo da dupla. Selecione uma carta e toque em Descartar para encerrar o turno. Use Dicas e Ordenar. A lupa ao lado do lixo mostra todas as cartas antes de recolhê-las. Toque no título Canastras para consultar a equipe rival.

Configurações contém quatro dificuldades: Aprendiz, Estrategista, Mestre e Oráculo. O jogo salva a partida atual automaticamente. A mesa disputa três rodadas, com total acumulado; o placar permite avançar após cada rodada.

## Regras adotadas

104 cartas sem curingões, 11 por jogador, dois mortos de 11. Sequências do mesmo naipe com pelo menos 3 cartas; 2 natural pode manter limpa, até um 2 substituto por jogo. Limpa com 7–12 cartas +200; suja +100; limpa 13 +500; Ás a Ás 14 +1000. Ás 15, 2 vale 20, 3–7 valem 5, 8–K valem 10. Para bater, morto retirado e canastra limpa. Não se bate descartando 2. Se acabar o monte, um morto disponível vira monte; sem mortos, encerra por pontos. Variante declarada na tela Como jogar.

## Código e evidências

- `java/src/main/java/com/buracoarcano/GameEngine.java`: regras, bots, pontuação e save.
- `BrowserBridge.java`: interface TeaVM com Web Crypto.
- `src/engine.js`: adaptador de apresentação; `src/app.js` e `src/style.css`: UI.
- `java/src/test/java/com/buracoarcano/ReleaseSmokeTest.java`: verificações rápidas dirigidas.
- `.github/workflows/release-check.yml`: build e fluxos reais no Chromium com capturas mobile/desktop.
- [Integração, evidências e limites](docs/INTEGRACAO-VISUAL-JAVA.md).

As suítes combinatórias históricas permanecem nas branches `qa/exhaustive-java-20261015` e `dev/buraco-arcano-premium`, suspensas conforme orientação do autor. Não são executadas neste workflow. Fontes antigas da alpha foram mantidas no histórico Git para consulta; não são carregadas pela nova mesa.
