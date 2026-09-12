#!/bin/bash
# Constrói o rootfs completo (Ubuntu 24.04), exporta como tar.gz versionado
# e já gera o manifest.json pronto (URL + tamanho + hash) apontando para uma
# release do GitHub em Kyra2214/SandBox — só falta criar a release de
# verdade e subir o asset (ver rootfs-builder/README.md).
set -euo pipefail

# Versão do rootfs. Sobrescreva com: VERSION=0.3.3 ./build.sh
# 0.2.0 foi a primeira versão da base Ubuntu (troca da base Alpine anterior,
# 0.1.x) — ver docs/roadmap-sandbox-fase0.md, item 0.1.
# 0.3.0 embute o catálogo inteiro de Plugins/Ferramentas (java/gradle, cmake,
# ninja, rust, go, ripgrep, fd, tree, htop, ollama etc.) já pré-instalado na
# imagem, pra "Instalar" no app ser instantâneo (só valida, não baixa nada).
# 0.3.1 adiciona testes Python, gerenciadores JS, clientes de banco,
# compressão adicional e ferramentas de terminal/rede para agentes.
# 0.3.2 remove Ollama do base; ele permanece como plugin opcional.
VERSION="${VERSION:-0.3.3}"
PLATFORM="${PLATFORM:-linux/arm64}"

# Repositório onde a release com o .tar.gz vai ser publicada.
GITHUB_REPO="Kyra2214/SandBox"
RELEASE_TAG="rootfs-v${VERSION}"

IMAGE_NAME="sandbox-rootfs-builder"
OUTPUT_DIR="../output"
OUTPUT_FILE="rootfs-ubuntu-${VERSION}.tar.gz"

mkdir -p "$OUTPUT_DIR"

echo "==> Construindo imagem Docker..."
# A rede bridge pode não estar disponível em ambientes restritos (por exemplo,
# quando o kernel não expõe a tabela raw do iptables). O build só precisa de
# acesso de saída para os repositórios apt, então a rede do host é suficiente.
docker build --network host --platform "$PLATFORM" -t "$IMAGE_NAME" .

echo "==> Criando container temporário..."
CONTAINER_ID=$(docker create --platform "$PLATFORM" "$IMAGE_NAME")

echo "==> Exportando filesystem..."
docker export "$CONTAINER_ID" | gzip > "$OUTPUT_DIR/$OUTPUT_FILE"

echo "==> Limpando container temporário..."
docker rm "$CONTAINER_ID" > /dev/null

echo "==> Calculando SHA-256 (para validar integridade no app)..."
sha256sum "$OUTPUT_DIR/$OUTPUT_FILE" > "$OUTPUT_DIR/$OUTPUT_FILE.sha256"
SHA256_HASH="$(cut -d' ' -f1 "$OUTPUT_DIR/$OUTPUT_FILE.sha256")"
SIZE_BYTES="$(stat -c%s "$OUTPUT_DIR/$OUTPUT_FILE" 2>/dev/null || stat -f%z "$OUTPUT_DIR/$OUTPUT_FILE")"

DOWNLOAD_URL="https://github.com/${GITHUB_REPO}/releases/download/${RELEASE_TAG}/${OUTPUT_FILE}"

echo "==> Gerando manifest.json..."
cat > "$OUTPUT_DIR/rootfs_manifest.json" << EOF
{
  "version": "${VERSION}",
  "arch": "arm64-v8a",
  "distro": "ubuntu-24.04",
  "url": "${DOWNLOAD_URL}",
  "sizeBytes": ${SIZE_BYTES},
  "sha256": "${SHA256_HASH}",
  "minAppVersion": "1.0.0"
}
EOF

ls -lh "$OUTPUT_DIR/$OUTPUT_FILE"

echo ""
echo "==> Pronto: $OUTPUT_DIR/$OUTPUT_FILE"
echo "==> Manifesto gerado: $OUTPUT_DIR/rootfs_manifest.json"
echo ""
echo "Próximo passo (precisa de rede/gh CLI, não roda aqui):"
echo "  gh release create ${RELEASE_TAG} \\"
echo "    \"$OUTPUT_DIR/$OUTPUT_FILE\" \\"
echo "    --repo ${GITHUB_REPO} \\"
echo "    --title \"Rootfs ${VERSION} (Ubuntu 24.04)\" \\"
echo "    --notes \"Rootfs completo (Ubuntu 24.04) para o Sandbox Mobile, arch arm64-v8a\""
echo ""
echo "Depois, copie $OUTPUT_DIR/rootfs_manifest.json para:"
echo "  app/src/main/res/raw/rootfs_manifest.json"
