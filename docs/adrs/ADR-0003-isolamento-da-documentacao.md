# ADR-0003 — Isolamento da documentação interna

- **Status:** Aceita
- **Data:** 2026-10-17

## 1. Contexto

A pasta `docs/` (board, arquitetura e ADRs) é um instrumento interno da equipe e **não é
entregue ao professor**. Somente o código-fonte, o `Makefile`, os testes e o `README.md` compõem a
entrega.

Referências à documentação dentro do código (por exemplo, "ver architecture.md", "ADR-0002",
"cartão T-07") criariam dependência de um material que o avaliador não terá: o leitor seria
induzido a procurar uma explicação fora do código. O avaliador espera um código que se explique
por si e que a equipe saiba defender oralmente.

## 2. Decisão

1. **Nenhum arquivo fora de `docs/` menciona a documentação interna**: nem `docs/`, nem ADRs,
   nem identificadores de cartões do board, nem `architecture.md`. Isso vale para o código-fonte,
   cabeçalhos, testes, `Makefile`, `README.md`, `.gitignore` e mensagens impressas pelos programas.
2. **Comentários somente onde o motivo não é óbvio.** Não há bloco de cabeçalho no início dos
   arquivos descrevendo o módulo; nomes de funções, variáveis e a própria estrutura do código
   devem comunicar o que é feito. O comentário explica o trecho que o código sozinho não explica.
3. **O `README.md` é autossuficiente**: contém apenas o necessário para compilar, executar e
   testar, sem detalhes específicos de máquina de qualquer membro da equipe.
4. A documentação em `docs/` pode referenciar livremente o código; o sentido inverso é vedado.

## 3. Alternativas consideradas

| Alternativa | Motivo da rejeição |
|---|---|
| Referenciar ADRs nos comentários para rastreabilidade | Depende de material não entregue; desloca a explicação para fora do código. |
| Cabeçalho descritivo em cada arquivo | Redundante com nomes bem escolhidos; tende a ficar desatualizado. |
| Entregar `docs/` junto com o código | Material de processo interno da equipe, não solicitado na avaliação. |

## 4. Consequências

**Positivas**
- O código entregue é autoexplicativo e independente de material externo.
- A rastreabilidade entre decisão e código continua existindo, mas apenas no sentido
  `docs/ → código`.

**Negativas**
- O vínculo entre um trecho de código e a ADR que o justifica precisa ser lembrado pela equipe
  (ou consultado em `docs/`) durante a preparação da apresentação.

## 5. Verificação

Antes da entrega, a busca abaixo, executada na raiz do projeto, não deve retornar resultados:

```bash
grep -rniE "docs/|\badrs?\b|adr-[0-9]|architecture\.md|board\.md|\bT-[0-9]+" --exclude-dir=docs --exclude-dir=.git --exclude-dir=build .
```
