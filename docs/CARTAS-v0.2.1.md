# Cartas maiores e organização manual — v0.2.1

- Cartas da mão ampliadas de 109×170 para 145×220 pixels na mesa base; números e naipes ampliados.
- Área da mão alargada; controles inferiores reposicionados para evitar sobreposição.
- Arraste com mouse ou dedo muda a posição da carta sem comprar, descartar ou baixar jogos.
- Movimento inferior a 8 pixels continua sendo toque de seleção. Arrastar não altera a seleção existente.
- O arraste funciona durante a vez dos bots. Segurar uma carta na lateral por 600 ms muda a página quando a mão tem mais de 13 cartas.
- A ordem escolhida é salva separadamente do estado de regras Java. É retomada ao reabrir o jogo e preservada nas próximas compras e turnos.
- Cancelamento do gesto e mudança de tamanho da tela restauram a posição anterior sem modificar cartas.
- APK de teste paralelo `com.buracoarcano.mobile.preview`, com versionCode 3 (0.2.1), para preservar a instalação e os dados da 0.2.0. A chave da primeira edição não estava no caminho coberto pelo cache; a nova edição usa uma chave em caminho explícito e configuração de assinatura própria.

Validação dirigida por eventos touch do Chromium em 412×915 e 360×800, com capturas reais e cenário de várias páginas. Sem reiniciar os testes exaustivos de combinações.

## Evidências

Testes de toque aprovados: https://github.com/sergiomrge-tech/Buraco-Arcano/actions/runs/38790721860 e https://github.com/sergiomrge-tech/Buraco-Arcano/actions/runs/38791113391 . Incluem arraste por touch real, seleção separada, cancelamento, persistência, compra, descarte, bots e arraste entre páginas. Nenhum erro JavaScript detectado.

[Cartas maiores](screenshots/android-cartas-maiores-v021.png) · [Arraste em execução](screenshots/android-arrastando-v021.png) · [Resultados](controles-v021-results.json). Capturas reais do Chromium com viewport de celular.

APK de teste compilado e verificado: https://github.com/sergiomrge-tech/Buraco-Arcano/actions/runs/38791113580 . Ainda não executado em aparelho Android físico neste ambiente.

Verificação final do APK: 2.048.204 bytes; SHA256 `8103c9188af62c13418b0fa58111e03cc64a201db5cc511c15bc6a841e53d098`; assinatura v2 e CRC aprovados. O JS e CSS extraídos do APK foram comparados com os mesmos fontes usados na verificação de toque. A edição se identifica como **Buraco Arcano Teste**, instalada paralelamente à 0.2.0. [Relatório do pacote](APK-v021-verificacao.json).
