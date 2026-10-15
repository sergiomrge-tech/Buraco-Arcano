# Buraco Arcano — As Quatro Casas

Protótipo **Web** do jogo brasileiro Buraco Aberto 2×2, com ambientação de fantasia original, partida local contra três bots e interface para navegador desktop/Android. Estado: **alpha de validação**; não é lançamento final nem possui multiplayer online.

## Como jogar

Abra `index.html` em um servidor estático (ex.: `python -m http.server 8000`, acesso em `http://localhost:8000`) ou em um hospedador de páginas estáticas. Em sua vez escolha **Comprar do monte** ou **Pegar o lixo**, selecione cartas para baixar sequências com três ou mais cartas do mesmo naipe, e selecione uma carta para **Descartar**. Para ampliar uma sequência da sua equipe, selecione carta(s) e toque a sequência correspondente. Use **Sugestão de jogada** para ajuda. São quatro níveis de dificuldade nas configurações.

## Implementação atual

- 104 cartas reais (duas cópias de 52); 11 cartas por jogador, dois mortos de 11 cartas, 1 descarte inicial e 37 cartas no monte.
- Fisher–Yates com `crypto.getRandomValues` e amostragem por rejeição para evitar viés na distribuição. Novas partidas geram embaralhamentos independentes.
- Turnos, compra, lixo completo, baixa e ampliação de sequências, um curinga 2 por sequência, Ás baixo/alto, canastra limpa/suja, mortos por equipe, batida com canastra, somatório e conservação de cartas.
- Bots com 4 parâmetros de estratégia: Fácil, Normal, Difícil e Especialista. **Não** são modelos neurais nem representam força de jogador profissional; os resultados não são garantidos contra humanos.
- Visual de fantasia CSS/SVG com controles touch e trilha sonora sintetizada localmente por Web Audio (sem arquivos externos).

## Testes (suspensos)

Por solicitação do autor em 15/10/2026, **não executar novos testes exaustivos de combinações**. Os arquivos de testes são mantidos apenas como referência histórica. Não existe workflow automático de CI para executá-los nesta versão. Antes da distribuição final, poderão ser feitos testes rápidos e dirigidos mediante solicitação.

## Tecnologia

HTML5 / CSS / JavaScript ES Modules (código-fonte em `src`). Para execução direta no navegador, JavaScript é o runtime web nativo. Java não é executado nativamente por navegadores atuais; uma futura edição Java exigiria back-end ou compilação para WebAssembly/JavaScript. Ainda não existe esse serviço no protótipo.

## Limites para produção

- A aparência **não foi homologada** na meta de fidelidade visual de 90% em relação à imagem de referência: requer comparação lado a lado, acabamento artístico e aprovação.
- Regras de Buraco variam regionalmente. A variante adotada precisa de revisão das exceções (uso especial do lixo, restrições de batida e critérios de pontuação) antes de competição online.
- Não há persistência da partida em curso, contas, multiplayer, anti-trapaça, rankings, animações 3D ou integração Android APK.
- A IA tem estratégias crescentes, mas falta teste estatístico de nível de habilidade.

## Licença de arte

Arte SVG e CSS produzida especialmente para este protótipo, sem copiar assets comerciais de Hearthstone.
