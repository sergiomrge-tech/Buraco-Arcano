# Mesa 0.2.2

Área explícita “Jogos da sua dupla”, ampliada e com botão “Baixar aqui”. Selecione pelo menos três cartas de uma sequência válida e use o botão ou arraste uma das cartas selecionadas à área. Para ampliar um jogo, selecione cartas e toque nele, ou arraste a carta até ele. O arraste dentro da mão continua reorganizando a ordem manual.

A validação continua no motor Java: não altera regras, baralho ou embaralhamento. Sequências inválidas permanecem na mão e mostram o erro. O progresso e a ordem manual são preservados.

Testes dirigidos: `tests/meld-area.mjs` e regressão `tests/hand-drag.mjs`, com eventos touch em 360 e 412 px. Sem testes exaustivos de combinações.
