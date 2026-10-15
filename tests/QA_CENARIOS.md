# Catálogo auditável de testes — Buraco Arcano (motor Java)

## Limite matemático importante
**Não é possível executar literalmente todas as partidas de 104 cartas.** O número de distribuições
possíveis supera qualquer capacidade prática de computação. A abordagem abaixo combina enumeração
exaustiva onde é viável, testes de fronteira, matrizes finitas, invariantes e simulação com sementes
reprodutíveis.

| Código consultável | Cenários automatizados | Estratégia |
|---|---|---|
| [ExhaustiveSequencesTest.java](../src/test/java/com/buracoarcano/ExhaustiveSequencesTest.java) | Todos os 182.104 trios físicos possíveis, 4 naipes, ases baixos e altos, 2 como curinga, sequências até 14 cartas, 15.000 mãos pseudoaleatórias | enumeração, oráculo independente e amostragem |
| [ActionsMatrixTest.java](../src/test/java/com/buracoarcano/ActionsMatrixTest.java) | 4 dificuldades × 2 compras × 4 jogadores; compra fora de vez, compra dupla, descarte ilegal, seleção repetida, baixar e ampliar jogos, morto | matriz de ações |
| [ScoringAndEndgameTest.java](../src/test/java/com/buracoarcano/ScoringAndEndgameTest.java) | 4 naipes × 13 ranks, canastras de 3 a 14 cartas, suja/limpa, pontos acumulados e nova rodada | casos de fronteira |
| [SeededSimulationTest.java](../src/test/java/com/buracoarcano/SeededSimulationTest.java) | 4 dificuldades × N sementes × M passos, compra, bots, passagem de turnos, conservação de 104 cartas | propriedade + replay |
| [GameEngineSmoke.java](../src/test/java/com/buracoarcano/GameEngineSmoke.java) | Smoke test básico e serialização JSON para o navegador | regressão |
| [visual.mjs](visual.mjs) | Chrome desktop, Android paisagem, Android retrato; clique, compra, descarte, posição de controles, capturas reais | integração / interface |
| [engine.test.mjs](engine.test.mjs) | Motor JavaScript legado (mantido como referência durante a migração) | regressão auxiliar |

Os arquivos são testes de código individuais, versionados e executados pelo workflow
[quality.yml](../.github/workflows/quality.yml).

## Como executar localmente

\`\`\`bash
mvn -B -ntp package -DskipTests
CP=target/test-classes:target/classes
java -cp "$CP" com.buracoarcano.GameEngineSmoke
java -cp "$CP" com.buracoarcano.ExhaustiveSequencesTest
java -cp "$CP" com.buracoarcano.ActionsMatrixTest
java -cp "$CP" com.buracoarcano.ScoringAndEndgameTest
java -cp "$CP" com.buracoarcano.SeededSimulationTest 30 180
npm test
npm run test:visual
\`\`\`

No Windows, substitua \`:\` por \`;\` no classpath. O GitHub Actions publica
\`qa-java-relatorios\` com logs e \`capturas-buraco-arcano\` com screenshots.

**Observações:** aprovação da suíte não equivale a prova matemática das regras completas
ou a 90% de semelhança visual. Questões regionais de regras (morto, sequência com dois 2,
batida e canastras especiais) precisam da variante oficial acordada. Bugs encontrados
devem ser corrigidos, e testes de regressão preservados.
