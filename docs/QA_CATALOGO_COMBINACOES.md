# Buraco Arcano — catálogo de testes combinatórios (Java)

## Objetivo e limite da palavra "todas"

O número de distribuições possíveis de 104 cartas em quatro mãos, dois mortos, lixo e monte é astronômico. Não é tecnicamente viável testar cada estado integral do jogo, nem afirmar 100% de cobertura sem especificar o domínio. A suíte adota três métodos complementares:

1. **Enumeração realmente exaustiva em domínio definido**: todos os **109.736 ternos distintos** extraídos das 88 cartas que não são Ás nem 2 nos dois baralhos. Oráculo independente: as três cartas são do mesmo naipe, possuem valores distintos e consecutivos.
2. **Cobertura de combinações estruturadas**: sequências limpas, cada posição de curinga, 4 naipes, 2 cópias, comprimentos de 3 a 11; pontuação, canastra de 13 cartas, dois 2 irregulares, Ás alto/baixo, quatro jogadores, quatro dificuldades.
3. **Partidas simuladas e reproduzíveis**: estados dinâmicos a partir de seeds fixas, preservação da identidade de 104 cartas após cada jogada, e proibição de turnos/entradas ilegais.

> QA reprodutível emprega `java.util.Random(seed)`. O jogo de produção usa `crypto.getRandomValues` e não utiliza os seeds do teste para embaralhar.

## Classes e IDs consultáveis

| Identificador no CSV | Cobertura | Método responsável |
|---|---|---|
| `T3-xxxxxx` | Todos os C(88,3)=109.736 ternos sem 2/Ás | `exhaustiveTriples` |
| `SEQ-xxxxx` | Sequências sem curinga, 4 naipes e 2 cópias | `validRunsAndJokers` |
| `COR-xxxxx-xx` | Cada posição de um curinga em sequência | `validRunsAndJokers` |
| `BON-xxxxx`, `C13-x` | Pontuação de canastras limpas/especiais | `validRunsAndJokers` |
| `ASE-BAIXO-x`, `ASE-ALTO-x` | Ás baixo e alto | `validRunsAndJokers` |
| `C2-x` | Dois curingas fora da posição natural | `validRunsAndJokers` |
| `DIST-xxxxx` | 104 cartas, 11 cada mão, 2 mortos e monte | `dealsAndTurns` |
| `SEED-xxxxx` | Repetição exata da distribuição com mesmo seed | `dealsAndTurns` |
| `FASE-xxxxx` | Impedir comprar/baixar fora de turno | `dealsAndTurns` |
| `TURNO-n-n` | Comprar 1 vez, baixar, descartar e mudar turno | `targetedIllegalMoves` |
| `INP-n-n` | IDs falsos, repetidos, lista vazia e índice ruim | `invalidMeldInputs` |
| `SIM-xxxxx` | Partidas simuladas nos 4 níveis de bot | `randomizedMatches` |

Cada linha do relatório `qa-combinatorio.csv` inclui código único do caso, categoria, seed, configuração, PASS/FAIL e detalhes, permitindo buscar o código exato para reproduzir a combinação. **Não são milhares de arquivos Java duplicados**: o código-fonte de cada família de combinações está no método indicado acima e o CSV identifica cada caso individual.

## Fontes e execução

- **Suíte**: `src/test/java/com/buracoarcano/CombinatorialQa.java`
- **Motor testado**: `src/main/java/com/buracoarcano/GameEngine.java`
- **Teste de integração Web**: `tests/visual.mjs`
- **Automação CI**: `.github/workflows/quality.yml`

Rodar no repositório:

~~~bash
mvn -B -ntp package -DskipTests
java -Dburaco.qaSeed=20261015 -Dburaco.fuzzGames=48 -cp target/test-classes:target/classes com.buracoarcano.CombinatorialQa
~~~

Ampliar o número de partidas por nível:

~~~bash
java -Dburaco.qaSeed=20261015 -Dburaco.fuzzGames=500 -cp target/test-classes:target/classes com.buracoarcano.CombinatorialQa
~~~

O parâmetro `fuzzGames` indica **partidas por nível**, portanto 500 significa até 2.000 partidas (500 × 4). O relatório será gravado em `reports/qa-combinatorio.csv`. Para reproduzir uma falha, use o seed gravado na coluna CSV, ou o `qaSeed` inicial do conjunto e o caso identificado.

No GitHub, cada execução de **Actions → Qualidade Java e capturas visuais** publica o artefato **qa-casos-individuais-java**, que contém o CSV completo. Assim, os códigos e resultados ficam consultáveis mesmo quando arquivos extensos não são versionados.

## Escopo que ainda requer validação adicional

- Distribuições arbitrárias de 104 cartas: amostradas em muitas seeds, **não exaustivas**.
- Jogadas de oponentes humanos online, reconexão e rede: ainda não implementadas.
- Variantes regionais não pertencentes ao Buraco Aberto escolhido.
- Comparação estética contra a imagem de referência e meta visual de 90%: requer inspeção humana das capturas reais.
- Estratégia perfeita de IA: quatro níveis presentes, mas "Lendário" não significa jogar de forma matematicamente ótima.

A CI falha ao encontrar um cenário que viola as propriedades esperadas. Todo resultado PASS deve ser confirmado na execução correspondente; a presença de código de teste sozinha não equivale a aprovação.
