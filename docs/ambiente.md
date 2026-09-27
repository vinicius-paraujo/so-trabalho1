# Configuração do ambiente com WSL

> **Fora do escopo do simulador de escalonamento.** Este documento trata do EDK II, da aplicação UEFI e do protokernel, material de apoio da disciplina. O simulador precisa apenas do JDK 21 e do Gradle Wrapper, conforme o `README.md`. O arquivo foi mantido no repositório por decisão da equipe (ADR 0008).

## 1. Objetivo

Descrever a preparação de uma máquina Windows para compilar o EDK II, construir a aplicação UEFI do trabalho e executá-la com QEMU por meio do WSL 2.

Este procedimento é destinado aos integrantes que optarem por WSL. Alterações no fluxo fornecido pelo professor devem ser preservadas em arquivos separados e documentadas em ADR.

## 2. Ambiente de referência

- Windows 10 versão 2004, build 19041 ou superior, ou Windows 11.
- Arquitetura x86-64.
- WSL 2.
- Ubuntu 24.04 LTS.
- Recomendação operacional: 8 GB de RAM, quatro núcleos lógicos e pelo menos 15 GB livres no disco do Windows.

Os valores de memória, processador e armazenamento são recomendações da equipe, não requisitos publicados pelo professor. O disco virtual do WSL é expansível, mas continua consumindo espaço físico do Windows.

Referências:

- [Instalação oficial do WSL](https://learn.microsoft.com/windows/wsl/install).
- [Preparação de ambiente de desenvolvimento no WSL](https://learn.microsoft.com/windows/wsl/setup/environment).

### 2.1 Estado verificado na máquina de Marcos

Verificação realizada em 20/09/2026:

- Windows 10 Pro, build 19045, x86-64;
- WSL 2 com Ubuntu 24.04.2 LTS;
- Git, GCC, G++, Make, Python, NASM e IASL disponíveis;
- `uuid-dev`, `parted`, `dosfstools`, OVMF e QEMU instalados;
- repositório EDK II no commit `6abc1c0658`;
- todos os submódulos na revisão esperada pelo EDK II;
- arquivo `brotli/c/common/constants.h` presente;
- BaseTools recompiladas com sucesso;
- 312 testes das BaseTools executados com resultado `OK`;
- `edksetup.sh` disponibiliza o comando `build`;
- QEMU 8.2.2 e arquivos OVMF exigidos pelo protokernel presentes;
- nó `/dev/kvm` presente, mas sem dispositivo KVM funcional para o QEMU;
- compilação validada; execução do protokernel inalterado não validada no WSL.

Na tentativa de execução, a criação e formatação da imagem EFI foram concluídas, mas o QEMU encerrou com `failed to initialize kvm: No such device`. Executar com `sudo` não corrige a ausência do backend KVM.

## 3. Instalação do WSL e do Ubuntu

Em PowerShell executado como administrador:

```powershell
wsl --install -d Ubuntu
```

Reiniciar o computador se solicitado. Após a inicialização do Ubuntu, criar o usuário Linux e verificar a versão:

```powershell
wsl --status
wsl --list --verbose
```

A distribuição deve aparecer com versão `2`. Caso esteja na versão `1`:

```powershell
wsl --set-version Ubuntu 2
```

## 4. Local de trabalho

Manter o EDK II no sistema de arquivos Linux, por exemplo em `/home/<usuario>/edk2`. Evitar compilar dentro de `/mnt/c`, pois essa localização pode reduzir desempenho e introduzir diferenças de permissões.

No Ubuntu:

```bash
cd ~
```

## 5. Dependências

Atualizar o índice de pacotes e instalar as ferramentas de compilação, criação da imagem EFI e execução:

```bash
sudo apt update
sudo apt install \
  build-essential \
  uuid-dev \
  nasm \
  acpica-tools \
  git \
  python3 \
  parted \
  dosfstools \
  ovmf \
  qemu-system-x86 \
  unzip
```

Finalidades principais:

| Pacote | Finalidade |
| --- | --- |
| `build-essential` | GCC, G++, Make e ferramentas básicas de compilação. |
| `uuid-dev` | Cabeçalhos e biblioteca UUID utilizados pelas BaseTools. |
| `nasm` | Montagem de código x86 e x86-64. |
| `acpica-tools` | Disponibilização do compilador ACPI `iasl`. |
| `parted` | Criação da tabela GPT e da partição EFI. |
| `dosfstools` | Disponibilização de `mkfs.fat`. |
| `ovmf` | Firmware UEFI utilizado pela máquina virtual. |
| `qemu-system-x86` | Emulação da máquina x86-64. |

Referências:

- [Instruções das BaseTools do EDK II](https://github.com/tianocore/edk2/blob/master/BaseTools/ReadMe.rst).
- [Pacote NASM do Ubuntu](https://packages.ubuntu.com/nasm).
- [Pacote ACPICA Tools do Ubuntu](https://packages.ubuntu.com/acpica-tools).
- [Pacote OVMF do Ubuntu](https://packages.ubuntu.com/noble/all/ovmf).
- [Pacote QEMU x86 do Ubuntu](https://packages.ubuntu.com/noble/qemu-system-x86).

## 6. Obtenção do EDK II

O procedimento atual recomendado pelo projeto TianoCore inicializa somente os submódulos diretos necessários:

```bash
cd ~
git clone https://github.com/tianocore/edk2.git
cd edk2
git submodule update --init
```

Não usar `--recursive` por padrão. O repositório oficial informa que a recursão também baixa submódulos internos que não são utilizados pelo EDK II.

Referência: [repositório oficial TianoCore/edk2](https://github.com/tianocore/edk2).

## 7. Compilação das BaseTools

Dentro do EDK II:

```bash
cd ~/edk2
make -C BaseTools -j"$(nproc)"
```

Configurar o terminal atual:

```bash
source edksetup.sh
```

Confirmar que o comando de build está acessível:

```bash
command -v build
```

O comando `source edksetup.sh` deve ser executado novamente em cada novo terminal usado para compilar o EDK II.

## 8. Inclusão e compilação da aplicação

Posicionar a aplicação no caminho definido pelo projeto, por exemplo:

```text
~/edk2/MdeModulePkg/Application/aulafinal/
```

Adicionar o arquivo INF à seção `[Components]` de `MdeModulePkg/MdeModulePkg.dsc`, conforme o roteiro do professor.

Compilar para X64 com GCC:

```bash
cd ~/edk2
source edksetup.sh
build \
  -a X64 \
  -t GCC \
  -p MdeModulePkg/MdeModulePkg.dsc \
  -m MdeModulePkg/Application/aulafinal/aulafinal.inf
```

O binário esperado é:

```text
~/edk2/Build/MdeModule/DEBUG_GCC/X64/aulafinal.efi
```

O nome e o caminho devem ser ajustados se o enunciado definitivo utilizar outro módulo.

## 9. Execução no QEMU

### 9.1 Verificação de KVM

A existência do arquivo `/dev/kvm` não comprova que o backend esteja funcional. A validação deve ser feita iniciando o QEMU com KVM. Nesta máquina, as seguintes opções presentes no material do professor falham:

```text
-enable-kvm
-cpu host,+x2apic
```

O erro observado foi:

```text
Could not access KVM kernel module: No such device
qemu-system-x86_64: failed to initialize kvm: No such device
```

Essa limitação não interfere na compilação.

### 9.2 Execução sem KVM

O QEMU em modo TCG inicializa sem KVM, mas não oferece o recurso x2APIC solicitado pelo protokernel. Portanto, remover `-enable-kvm` ou trocar apenas o modelo de CPU não permite executar a baseline inalterada.

Se o protokernel for confirmado como parte do trabalho, a equipe deverá escolher e documentar uma destas alternativas:

1. executar em Linux nativo com KVM;
2. utilizar uma plataforma que forneça virtualização aninhada funcional;
3. adaptar o protokernel para outro controlador de interrupções, mudança que exige análise e ADR.

Até essa confirmação, o WSL é considerado validado apenas para compilação.

Referências:

- [Execução de OVMF com QEMU](https://github.com/tianocore/tianocore.github.io/wiki/How-to-run-OVMF).
- [Configuração avançada e virtualização aninhada do WSL](https://learn.microsoft.com/windows/wsl/wsl-config).

## 10. Correção do erro de Brotli

### 10.1 Sintoma

Falha durante `make -C BaseTools`:

```text
BrotliCompress.c: fatal error: ./brotli/c/common/constants.h: No such file or directory
```

### 10.2 Causa

O submódulo Brotli não foi inicializado, está em revisão diferente da registrada pelo EDK II ou teve seus arquivos removidos. O problema não é causado pelo WSL.

### 10.3 Diagnóstico

```bash
cd ~/edk2
git submodule status
test -f BaseTools/Source/C/BrotliCompress/brotli/c/common/constants.h
```

Interpretação do primeiro caractere exibido por `git submodule status`:

- espaço: submódulo na revisão esperada;
- `-`: submódulo não inicializado;
- `+`: submódulo em revisão diferente;
- `U`: conflito de mesclagem.

### 10.4 Recuperação normal

```bash
cd ~/edk2
git submodule sync
git submodule update --init
```

### 10.5 Recuperação de submódulo inconsistente

Somente quando a equipe confirmar que não existem alterações autorais dentro do submódulo:

```bash
cd ~/edk2
git submodule update --init --force \
  BaseTools/Source/C/BrotliCompress/brotli
git submodule update --init
```

`--force` descarta alterações existentes no submódulo indicado. Não utilizar esse parâmetro como procedimento rotineiro.

Após a recuperação:

```bash
test -f BaseTools/Source/C/BrotliCompress/brotli/c/common/constants.h
make -C BaseTools -j"$(nproc)"
```

## 11. Inconsistência conhecida no protokernel

No material atualmente disponível:

- `buildprog.sh` gera `draw.app`;
- `run.sh` tenta copiar `draw.bin`.

O fluxo falhará até que o nome esperado seja confirmado. A equipe não deve alterar silenciosamente o material. A correção deve ser feita em cópia versionada, justificada e registrada em ADR.

## 12. Verificação final

Executar:

```bash
gcc --version
make --version
nasm --version
iasl -v
python3 --version
qemu-system-x86_64 --version
test -f /usr/share/OVMF/OVMF_CODE_4M.fd
test -f /usr/share/OVMF/OVMF_VARS_4M.ms.fd
test -f ~/edk2/BaseTools/Source/C/bin/GenFv
```

O ambiente somente deve ser considerado preparado quando:

- todas as ferramentas responderem sem erro;
- as BaseTools forem compiladas;
- a aplicação gerar o arquivo `.efi` esperado;
- a imagem EFI puder ser criada;
- a estratégia de execução com ou sem KVM estiver documentada e validada.
