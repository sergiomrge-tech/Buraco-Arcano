# Buraco Arcano — APK de teste 0.2.0

Aplicativo Android `com.buracoarcano.mobile`, com a mesma mesa vertical da edição web, motor Java compilado e todos os arquivos incluídos no APK. Modo local contra três bots; não inclui multiplayer.

- Android mínimo: 7.0 (API 24), com Android System WebView atualizado.
- API de compilação/alvo: 35; Java 17; AGP 8.7.3; Gradle 8.9.
- Tela vertical e imersiva; botão Voltar fecha o diálogo aberto ou confirma saída.
- Salvamento no armazenamento local do WebView, preservado ao sair e reabrir.
- `WebViewAssetLoader` fornece origem HTTPS interna e Web Crypto para embaralhamento sem semente fixa.
- Sem permissão de internet, armazenamento externo, câmera, microfone ou contatos. Todos os assets são locais.
- Service worker não é necessário na versão nativa; ele continua disponível na versão de navegador.

## Build

```sh
npm run check
npm run build
node scripts/prepare-android.mjs
cd android
gradle :app:assembleDebug
```

O workflow `.github/workflows/android-apk.yml` provisiona o SDK, compila e verifica a assinatura, alinhamento, CRC e presença do motor Java. O artifact contém o APK e os relatórios.

Assinatura de teste/debug, sem configuração de Play Store. A chave debug é preservada em cache do workflow para atualizações de teste; não é uma chave de distribuição pública de produção.

Os fluxos da mesa foram aprovados em Chromium com viewport mobile. A compilação e as verificações do pacote não equivalem a execução em aparelho Android físico. O APK é fornecido para esse teste no celular do usuário.

## APK gerado e verificado

Build aprovado: https://github.com/sergiomrge-tech/Buraco-Arcano/actions/runs/38788473747

- Arquivo: `Buraco_Arcano_v0.2.0.apk`
- Tamanho: 2.045.856 bytes.
- SHA256: `83c456a7512527a07a3693060a6baded0b5eacc689fbf4a4fc386bd1bdd28f1b`.
- Assinatura v2 válida; chave RSA 2048; certificado SHA256 `6caa6a23beccd3a6c6e5055f2f7223770d4cb807b299cdbbb2cbcc00d47e393e`.
- zipalign aprovado; CRC aprovado; assets essenciais e classes.dex presentes.
- Motor Java no APK idêntico ao validado na versão web (`89ad4e674c8175d3e87ef899c42ec5c62c6d79a7e15bec819adf838534c904f9`).
- Application ID e launcher conferidos: `com.buracoarcano.mobile`, `MainActivity`.

O APK foi disponibilizado ao autor para instalação e teste no aparelho. Não houve execução em celular físico neste ambiente.
