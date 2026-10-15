# Buraco Arcano — As Quatro Casas

Jogo de Buraco Aberto brasileiro para navegador, com mesa de fantasia original inspirada em jogos de cartas premium.

**Em desenvolvimento.** Esta branch de trabalho não representa aprovação visual de 90% de fidelidade à referência enviada pelo proprietário. A validação visual deve comparar screenshots reais do navegador com a referência original.

## Implementado nesta base de desenvolvimento

- 4 jogadores em 2 duplas: você e Lyra contra Morgath e Valtor.
- 104 cartas, 11 por jogador, 2 mortos de 11, 37 no monte e 1 no lixo inicial.
- Embaralhamento Fisher-Yates com Web Crypto a cada nova partida.
- Compra do monte ou do lixo aberto, descarte, baixar sequências e ampliar jogos com toque.
- Sequências do mesmo naipe; até um 2 como curinga fora da posição natural; canastras com 7 ou mais.
- Canastra limpa exigida para a batida final; morto por dupla; pontuação até 3.000.
- IA com níveis Fácil, Normal, Difícil e Lendário.
- Mesa em CSS/SVG e layout paisagem Android.
- Testes de motor e screenshots do navegador via GitHub Actions/Playwright.

## Rodar pelo navegador

Inicie um servidor web a partir da pasta do jogo, por exemplo:

    python3 -m http.server 8173

Abra http://localhost:8173 no navegador.

Para testar as regras use npm test. Para gerar screenshots, instale Playwright e Chromium e execute npm run test:visual.

## Ainda pendente

- Comparação lado a lado e polimento visual até o critério de semelhança combinado.
- Auditoria aprofundada de casos extremos das regras de Buraco.
- Online com amigos, persistência de salas e matchmaking.
- Arte final e animações ricas de distribuição.
- Validação final de build web e futuro APK.

Referências das regras: https://jogada.com.br/regras/buraco-aberto/ e https://www.jogosdorei.com.br/regras-buraco.php.

**Branch de trabalho:** dev/buraco-arcano-premium. Não mesclar em main antes da aprovação visual.
