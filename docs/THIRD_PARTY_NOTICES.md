# Avisos de terceiros

Este projeto empacota, dentro do APK, binários de terceiros que não fazem
parte do código-fonte deste repositório. Nenhum deles foi modificado — são
usados como distribuídos originalmente pelo projeto de origem.

---

## proot

- **O que é**: ferramenta usada para emular `chroot`/namespaces sem privilégios
de root, permitindo rodar o rootfs Linux dentro da sandbox do app Android.
- **Projeto original**: https://proot-me.github.io/ (proot-me/proot no GitHub)
- **Licença**: GPL-2.0
- **Origem do binário empacotado**: pacote `proot` do repositório oficial do
Termux (`termux-main`).
- **Pacote exato**: `proot_5.1.107.92_aarch64.deb`
- **URL de origem**: https://cdimage.debian.org/mirror/termux.dev/apt/termux-main/pool/main/p/proot/proot_5.1.107.92_aarch64.deb
- **SHA-256 do binário empacotado**: `ea47e17da8e6ff4882c169c6508861e5b4be9227e477c6020f4f14facc85c10d`
- **Local no APK**: `jniLibs/arm64-v8a/libproot.so`
- **Como foi obtido**: `scripts/fetch-proot.sh`; o ELF foi apenas extraído do
`.deb` e renomeado para a convenção de empacotamento Android, sem modificação.
- **Código-fonte**: https://github.com/proot-me/proot

O arquivo `docs/proot-binary-provenance.txt` mantém o registro estruturado da
origem e do hash usados nesta versão.
