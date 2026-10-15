# Relatório de QA combinatório — Buraco Arcano

**Data da execução:** 15/10/2026 (São Paulo)  
**Branch auditada:** `qa/exhaustive-java-20261015`  
**Commit verificado:** `c41c3e9ec4d48e551a76ff90504ce40e7e5a49e3`  
**Execução CI:** [GitHub Actions #38780378414](https://github.com/sergiomrge-tech/Buraco-Arcano/actions/runs/38780378414) — **SUCESSO**

## Resultados medidos (auditoria Java)

| Grupo | Casos | Resultado |
|---|---:|---|
| Todas as trincas físicas de 104 cartas, `C(104,3)` | 182.104 | PASS |
| Sequências por tamanho, naipe e posição de curinga | 9.644 | PASS |
| Distribuição e embaralhamento | 8.000 | PASS |
| Turnos, fases e permissões | 4.900 | PASS |
| Pontuação e canastras limpas/sujas | 650 | PASS |
| Mortos, batida e fim do monte | 1.502 | PASS |
| Criação/ampliação/rejeição de jogos | 1.650 | PASS |
| Simulações completas dos bots (4 dificuldades) | 72 | PASS |
| **Total da auditoria independente** | **208.522** | **0 falhas** |

Nas **72 rodadas completas**, foram verificados **3.690 turnos**. Teste adicional `CombinatorialQa`: **120.000 casos, 0 falhas**, abrangendo 109.736 trincas sem A/2, sequências, pontos, distribuição, partidas e estados. Esse lote sobrepõe parcialmente o principal; não some os totais como se fossem todos casos distintos.

## Códigos independentes por tipo de cenário

- [Todas as trincas com oráculo independente](../src/test/java/com/buracoarcano/ThreeCardExhaustiveTest.java)
- [Sequências e curingas](../src/test/java/com/buracoarcano/ExtendedSequencesTest.java)
- [Distribuição e embaralhamento](../src/test/java/com/buracoarcano/DealAndShuffleTest.java)
- [Turnos e permissões](../src/test/java/com/buracoarcano/TurnAndPermissionsTest.java)
- [Pontos e canastras](../src/test/java/com/buracoarcano/ScoringAndCanastaTest.java)
- [Morto e batida](../src/test/java/com/buracoarcano/DeadPileAndExitTest.java)
- [Criação e ampliação de jogos](../src/test/java/com/buracoarcano/MeldMutationTest.java)
- [Rodadas completas, dificuldade e reprodução](../src/test/java/com/buracoarcano/BotStressTest.java)
- [Orquestrador da auditoria](../src/test/java/com/buracoarcano/AuditAllTests.java)
- [Gerador de CSV detalhado](../src/test/java/com/buracoarcano/CombinatorialQa.java)

## Relatórios individuais

Os relatórios `qa-trincas-104.csv` e `qa-combinatorio.csv` registram identificadores e resultados de casos testados. Estão no [artefato da execução validada](https://github.com/sergiomrge-tech/Buraco-Arcano/actions/runs/38780378414/artifacts/11995830590) do GitHub Actions. Atenção: os artefatos do Actions têm retenção limitada; os arquivos de código e este relatório permanecem versionados no GitHub.

Cada classe acima possui `main()` e pode ser executada separadamente; a execução conjunta usa `AuditAllTests`.

## Erros encontrados e corrigidos durante a implantação

1. Validação de sequência A–2–3–…–K–A com dois ases físicos.
2. Situação excepcional de mão vazia sem morto disponível, anteriormente capaz de travar um bot.
3. Preparação das mãos em testes garantindo conservação física das 104 cartas.

As correções fazem parte das branches de desenvolvimento e auditoria e foram submetidas a novos testes.

## Escopo e limites

**Isto não é uma prova de todas as partidas possíveis com 104 cartas.** A auditoria enumera integralmente as trincas e usa combinações delimitadas e amostras reproduzíveis para os demais estados. Também não mede fidelidade visual de 90% à referência artística, latência real em aparelhos variados, partidas online nem comportamento em todas as regras regionais do Buraco. Esses itens permanecem como validações separadas.
