# Cartas maiores e organização manual — v0.2.1

- Cartas da mão ampliadas de 109×170 para 145×220 pixels na mesa base; números e naipes ampliados.
- Área da mão alargada; controles inferiores reposicionados para evitar sobreposição.
- Arraste com mouse ou dedo muda a posição da carta sem comprar, descartar ou baixar jogos.
- Movimento inferior a 8 pixels continua sendo toque de seleção. Arrastar não altera a seleção existente.
- O arraste funciona durante a vez dos bots. Segurar uma carta na lateral por 600 ms muda a página quando a mão tem mais de 13 cartas.
- A ordem escolhida é salva separadamente do estado de regras Java. É retomada ao reabrir o jogo e preservada nas próximas compras e turnos.
- Cancelamento do gesto e mudança de tamanho da tela restauram a posição anterior sem modificar cartas.
- APK de atualização com o mesmo application ID e versionCode 3 (0.2.1).

Validação dirigida por eventos touch do Chromium em 412×915 e 360×800, com capturas reais e cenário de várias páginas. Sem reiniciar os testes exaustivos de combinações.
