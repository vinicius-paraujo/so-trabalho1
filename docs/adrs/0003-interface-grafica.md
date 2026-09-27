# ADR 0003: Adoção de interface gráfica

- Status: Aceita
- Data: 20/09/2026
- Responsável pela proposta: Marcos

## Contexto

O enunciado permite uma interface gráfica como atividade adicional com possibilidade de bonificação. A equipe decidiu incorporá-la à entrega.

A interface não pode comprometer a correção dos algoritmos nem duplicar regras de escalonamento.

## Decisão

1. A solução terá interface gráfica.
2. A interface será implementada após a validação do núcleo obrigatório.
3. A camada gráfica consumirá os resultados da simulação sem conter regras de seleção, preempção, aging ou cálculo de métricas.
4. A tecnologia gráfica será definida em decisão posterior, antes do início de sua implementação. Definida no [ADR 0007](0007-tecnologia-da-interface-grafica.md): Swing.

## Consequências

- A interface passa a integrar o escopo planejado do projeto.
- O cronograma deverá reservar tempo para implementação e testes da camada gráfica.
- O núcleo continuará executável e testável independentemente da interface.

