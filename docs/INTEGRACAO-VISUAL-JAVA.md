# Integração da mesa vertical e motor Java — 15/10/2026

A mesa vertical foi recuperada do Site já existente e passou a usar o GameEngine Java da branch de auditoria, compilado com TeaVM. O adaptador JavaScript apresenta o estado; não executa uma segunda simulação paralela de regras.

## Implementado nesta integração

- Arte da taverna, retratos, molduras e cartas no layout vertical escolhido. Os componentes clicáveis são elementos HTML, com valores atualizados pela partida real.
- 104 cartas; dois mortos; compra de monte/lixo; visualização do lixo inteiro; sequências; curinga 2 natural; Ás baixo/alto/Ás a Ás; canastras; três rodadas; placar e reinício.
- Embaralhamento Java alimentado por Web Crypto com rejeição do viés modular. Nenhuma semente fixa em produção.
- Quatro dificuldades de bots aplicadas à mesa atual. Buscam sequências e avaliam cartas públicas; não acessam mãos ocultas nas decisões.
- Save local versionado, com validação de IDs, conservação de 104 cartas, fases e sequências antes da restauração Java.
- PWA, manifesto e cache dos arquivos para reabrir offline após primeiro carregamento bem-sucedido.
- Sons opcionais e redução de animação.
- Valor do 2 corrigido para 20; prevenção de jogada que deixa uma carta sem saída legal; proibição de bater descartando 2.

## Verificação

50 verificações Java dirigidas executadas localmente com sucesso. Quatro partidas completas de três rodadas (12 rodadas) foram acompanhadas até o encerramento e restauração do save final, sem travamentos. A compilação TeaVM foi aprovada localmente e no GitHub, e o motor compilado foi executado com compra, dica, sequência, restauração e turnos. Não foi retomada a enumeração exaustiva de combinações. As evidências históricas de 328.522 verificações pertencem ao motor anterior, antes das mudanças desta integração, e não constituem resultado desta versão.

O workflow `release-check.yml` compila o motor real, roda somente a suíte dirigida e verifica compra, seleção, descarte, dificuldade, salvamento, turnos dos bots e recarga offline em três resoluções. Gera screenshots reais em artifacts, inclusive em falhas.

## Limites e critérios de conclusão

- Multiplayer online com amigos ainda não implementado.
- Sem homologação numérica de 90% de semelhança visual: a arte e composição são compartilhadas com a referência, mas a porcentagem não é uma métrica estabelecida. As capturas reais devem ser comparadas visualmente.
- O tabuleiro é intencionalmente vertical. Em desktop/paisagem, fica centralizado com margens.
- Save e cache offline dependem de armazenamento disponível no navegador; limpar dados do site remove a partida.
- Sem APK nesta integração, sem conta, ranking ou servidor de salas.

Esta integração não deve ser descrita como o projeto completo enquanto online e gate visual estiverem pendentes.

## Evidência real no GitHub

Workflow inicial aprovado: https://github.com/sergiomrge-tech/Buraco-Arcano/actions/runs/38784963677 . Passou compra/seleção/descarte por toque, dificuldade, save, três bots, lixo e recarga offline em 412×915, 360×800 e 1200×750. Sem erros JavaScript. Capturas reais armazenadas no artifact `buraco-arcano-web-e-capturas`. Não equivalem a teste de aparelho Android físico.

Workflow de confirmação aprovado após ajustes visuais: https://github.com/sergiomrge-tech/Buraco-Arcano/actions/runs/38785548524 . Capturas reais: [mesa](screenshots/android-mesa.png), [após três bots](screenshots/android-jogando.png). Relatório estruturado: [browser-results.json](browser-results.json).

Motor compilado do artifact CI é idêntico ao compilado local: SHA256 `89ad4e674c8175d3e87ef899c42ec5c62c6d79a7e15bec819adf838534c904f9`. O bundle é incluído no repositório para permitir servir a raiz diretamente sem instalar Maven.

A versão local foi publicada com sucesso no Site existente. A audiência privada foi preservada.

Cenário adicional de baixa de sete cartas e persistência da canastra aprovado no workflow https://github.com/sergiomrge-tech/Buraco-Arcano/actions/runs/38786376865 . A distribuição foi preparada pelo teste conservando 104 cartas; a compra, seleção, baixa, pontuação e restauração foram executadas na interface e motor reais. [Captura real do cenário automatizado](screenshots/android-canastra-real.png).
