# ADR 0008: Conteúdo da entrega

- Status: Aceita
- Data: 27/09/2026
- Responsável pela proposta: Tiago

## Contexto

O critério do card T16 determina que somente arquivos pertencentes à entrega permaneçam no diretório do projeto. O repositório contém, além do simulador:

- `docs/Notas de aula/`: enunciado, notas de aula, exemplos compactados, guia do EDK II e uma página salva do SIGAA (`index.jsf`);
- `docs/ambiente.md`: preparação do ambiente EDK II/UEFI/QEMU para o protokernel.

Somente o enunciado e as notas de escalonamento são fontes normativas deste trabalho (ADR 0001).

## Alternativas consideradas

1. Manter apenas o enunciado e as notas de escalonamento.
2. Remover todo o material de apoio e apenas citá-lo.
3. Manter todo o material e identificar o que está fora do escopo.

## Decisão

A equipe escolheu a alternativa 3:

1. `docs/Notas de aula/` e `docs/ambiente.md` permanecem no repositório como material de apoio.
2. `docs/ambiente.md` recebe um aviso de que está fora do escopo do simulador.
3. O `README.md` separa o material do simulador do material de apoio.
4. O pacote gerado por `gradlew distZip` contém somente a aplicação, pois o material de apoio não participa da construção.

## Consequências

- As fontes citadas nos documentos continuam acessíveis no próprio repositório.
- O repositório é maior que o estritamente necessário. O critério do T16 é atendido pela separação explícita, e não pela remoção.
- Quem avaliar a entrega precisa ler o `README.md` para distinguir o que pertence ao simulador.
