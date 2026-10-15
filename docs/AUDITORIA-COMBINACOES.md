# Auditoria de combinações — Buraco Arcano (Java)

Esta auditoria atua no motor Java real do Buraco Arcano Premium,
em src/main/java/com/buracoarcano/GameEngine.java, sem alterar o projeto
visual do jogo.

## Resultado validado em 15/10/2026

Execução aprovada e logs:
https://github.com/sergiomrge-tech/Buraco-Arcano/actions/runs/38780378414

| Família de testes | Casos aprovados |
| --- | ---: |
| Todas as trincas físicas C(104,3), com oráculo independente | 182.104 |
| Sequências de 3 a 14 cartas, naipes, posições de curinga e inválidas | 9.644 |
| Embaralhamento e distribuição | 8.000 |
| Turnos, fases e permissões | 4.900 |
| Pontuação, canastras especiais e curingas | 650 |
| Mortos, fim de rodada, batida, reposição de monte | 1.502 |
| Baixar/estender jogos e rejeitar jogadas inválidas | 1.650 |
| Partidas automatizadas, bots, reprodução | 72 |
| **Total das suítes principais** | **208.522** |
| Catálogo combinatório adicional CombinatorialQa | 120.000 |
| **Total das duas baterias** | **328.522** |

Os testes de partidas cobrem 4 níveis de IA, somando 120 jogos com
sementes reproduzíveis (72 + 48 no catálogo extra).

## Código separado por categoria

- src/test/java/com/buracoarcano/ThreeCardExhaustiveTest.java
- src/test/java/com/buracoarcano/ExtendedSequencesTest.java
- src/test/java/com/buracoarcano/DealAndShuffleTest.java
- src/test/java/com/buracoarcano/TurnAndPermissionsTest.java
- src/test/java/com/buracoarcano/ScoringAndCanastaTest.java
- src/test/java/com/buracoarcano/DeadPileAndExitTest.java
- src/test/java/com/buracoarcano/MeldMutationTest.java
- src/test/java/com/buracoarcano/BotStressTest.java
- src/test/java/com/buracoarcano/CombinatorialQa.java
- src/test/java/com/buracoarcano/TestSupport.java
- src/test/java/com/buracoarcano/AuditAllTests.java

## Consultar cada combinação executada

O GitHub Actions publica, em cada execução, o artifact
auditoria-combinatoria-java com dois arquivos:

1. reports/qa-trincas-104.csv — 182.104 linhas individuais com ID,
   três cartas físicas, resultado esperado, resultado obtido e PASS/FAIL.
2. reports/qa-combinatorio.csv — mais 120.000 verificações identificadas
   com categoria, seed, configuração e resultado.

O código dos testes é permanente no repositório. Os CSV são
**regeneráveis** e os artifacts ficam disponíveis por 90 dias.

## Executar localmente (Java 17)

Na raiz:

    mkdir -p out reports
    javac -encoding UTF-8 -d out src/main/java/com/buracoarcano/GameEngine.java src/test/java/com/buracoarcano/*.java
    java -Xmx1024m -cp out com.buracoarcano.AuditAllTests
    java -Xmx1024m -Dburaco.fuzzGames=12 -Dburaco.qaSeed=20261015 -Dburaco.report=reports/qa-combinatorio.csv -cp out com.buracoarcano.CombinatorialQa

Executar testes isoladamente:

    java -cp out com.buracoarcano.ThreeCardExhaustiveTest
    java -cp out com.buracoarcano.DeadPileAndExitTest
    java -cp out com.buracoarcano.BotStressTest

Workflow automático: .github/workflows/combinatorial-audit.yml

## Ajustes identificados e incluídos

- Sequência especial de 14 cartas (A-2-3-...-K-A) agora aceita
  os dois ases em extremidades distintas.
- Índices negativos menores que -1 não criam jogos indevidamente.
- Se um jogador ficar sem cartas quando os dois mortos
  já não estão disponíveis, a rodada encerra por pontos,
  impedindo um bot de ficar travado.
- Regressão permanente específica desse travamento incluída.

## Limites da cobertura

Não é matematicamente viável enumerar todas as ordens do baralho
completo (104 fatorial) e todos os estados possíveis do jogo.
São **exaustivas** as trincas C(104,3) e as famílias de intervalos/naipes/
posições delimitadas por ExtendedSequencesTest. O restante usa cenários
sistemáticos, invariantes e partidas com sementes variadas.

Resultados positivos não provam que todas as variantes regionais do
Buraco estejam contempladas e não são aprovação da arte, da interface,
do jogo online ou da fidelidade visual de 90% à referência aprovada.
